package org.certis.studyplatform.shared.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyExecutor {

    private final IdempotencyStore idempotencyStore;
    private final IdempotencyProperties properties;

    public <T> T execute(IdempotencyKeyContext context, String payloadHash, Supplier<T> supplier) {
        Instant now = Instant.now();
        FastPathResult<T> fastPathResult = fastPath(idempotencyStore.find(context).orElse(null), payloadHash, now);
        if (fastPathResult.resolved()) {
            return fastPathResult.result();
        }

        boolean lockAcquired = idempotencyStore.tryLock(
                context,
                Duration.ZERO,
                Duration.ofSeconds(properties.getLockLeaseSeconds())
        );
        boolean processingMarked = false;

        if (!lockAcquired) {
            throw new DomainException(ExceptionStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }

        try {
            now = Instant.now();
            IdempotencyRecord existing = idempotencyStore.find(context).orElse(null);
            fastPathResult = fastPath(existing, payloadHash, now);
            if (fastPathResult.resolved()) {
                return fastPathResult.result();
            }

            Instant startedAt = now;
            idempotencyStore.save(
                    context,
                    IdempotencyRecord.builder()
                            .status(IdempotencyStatus.PROCESSING)
                            .payloadHash(payloadHash)
                            .startedAt(startedAt)
                            .updatedAt(startedAt)
                            .build(),
                    Duration.ofSeconds(properties.getRecordTtlSeconds())
            );
                    processingMarked = true;

            T result = supplier.get();
            Runnable completionWrite = () -> idempotencyStore.save(
                    context,
                    IdempotencyRecord.builder()
                            .status(IdempotencyStatus.COMPLETED)
                            .payloadHash(payloadHash)
                            .startedAt(startedAt)
                            .updatedAt(Instant.now())
                            .responseStatusCode(200)
                            .responseMessage("COMPLETED")
                            .responseData(result)
                            .build(),
                    Duration.ofSeconds(properties.getRecordTtlSeconds())
            );

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        completionWrite.run();
                    }
                });
            } else {
                completionWrite.run();
            }

            return result;
        } catch (RuntimeException ex) {
            if (processingMarked) {
            idempotencyStore.save(
                context,
                IdempotencyRecord.builder()
                    .status(IdempotencyStatus.FAILED_RETRYABLE)
                    .payloadHash(payloadHash)
                    .startedAt(Instant.now())
                    .updatedAt(Instant.now())
                    .responseStatusCode(500)
                    .responseMessage(ex.getMessage())
                    .build(),
                Duration.ofSeconds(properties.getRecordTtlSeconds())
            );
            }
            throw ex;
        } finally {
            idempotencyStore.unlock(context);
        }
    }

    private void validatePayloadHash(IdempotencyRecord existing, String currentHash) {
        if (!Objects.equals(existing.getPayloadHash(), currentHash)) {
            throw new DomainException(ExceptionStatus.IDEMPOTENCY_PAYLOAD_MISMATCH);
        }
    }

    private <T> FastPathResult<T> fastPath(IdempotencyRecord existing, String payloadHash, Instant now) {
        if (existing == null) {
            return FastPathResult.unresolved();
        }

        validatePayloadHash(existing, payloadHash);

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            @SuppressWarnings("unchecked")
            T replayed = (T) existing.getResponseData();
            return FastPathResult.resolved(replayed);
        }

        if (existing.getStatus() == IdempotencyStatus.PROCESSING
                && !existing.isStale(now, properties.getProcessingStaleSeconds())) {
            throw new DomainException(ExceptionStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }

        return FastPathResult.unresolved();
    }

    private record FastPathResult<T>(boolean resolved, T result) {
        private static <T> FastPathResult<T> resolved(T result) {
            return new FastPathResult<>(true, result);
        }

        private static <T> FastPathResult<T> unresolved() {
            return new FastPathResult<>(false, null);
        }
    }
}
