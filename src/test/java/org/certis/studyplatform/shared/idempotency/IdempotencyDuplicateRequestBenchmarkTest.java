package org.certis.studyplatform.shared.idempotency;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyDuplicateRequestBenchmarkTest {

    private static final int TOTAL_REQUESTS = 1_000;
    private static final long UPLOAD_DURATION_MILLIS = 25L;

    @Test
    void benchmarkDuplicateUploadRequestsWith1000Requests() throws Exception {
        long[] arrivalPlanMillis = createArrivalPlanMillis();

        BenchmarkResult before = runWithoutIdempotency(arrivalPlanMillis);
        BenchmarkResult current = runWithLegacyExecutor(arrivalPlanMillis);
        BenchmarkResult optimized = runWithOptimizedExecutor(arrivalPlanMillis);

        printSummary(before, current, optimized);

        assertThat(before.uploadExecutions()).isEqualTo(TOTAL_REQUESTS);
        assertThat(before.duplicateFileCount()).isEqualTo(TOTAL_REQUESTS - 1L);
        assertThat(before.redisOpsPerRequest()).isZero();

        assertThat(current.uploadExecutions()).isEqualTo(1L);
        assertThat(current.duplicateFileCount()).isZero();
        assertThat(current.blockedDuplicateRatePercent()).isEqualTo(100.0d);

        assertThat(optimized.uploadExecutions()).isEqualTo(1L);
        assertThat(optimized.duplicateFileCount()).isZero();
        assertThat(optimized.blockedDuplicateRatePercent()).isEqualTo(100.0d);
        assertThat(optimized.redisOpsPerRequest()).isLessThan(current.redisOpsPerRequest());
    }

    private BenchmarkResult runWithoutIdempotency(long[] arrivalPlanMillis) throws Exception {
        RequestTracker tracker = new RequestTracker();

        long elapsedMillis = executeRequests(arrivalPlanMillis, requestIndex -> {
            sleepQuietly(arrivalPlanMillis[requestIndex]);
            tracker.upload("legacy/" + requestIndex + "/" + UUID.randomUUID());
        });

        return BenchmarkResult.from("before", tracker, 0L, elapsedMillis);
    }

    private BenchmarkResult runWithLegacyExecutor(long[] arrivalPlanMillis) throws Exception {
        CountingIdempotencyStore store = new CountingIdempotencyStore();
        IdempotencyProperties properties = createProperties();
        LegacyIdempotencyExecutor executor = new LegacyIdempotencyExecutor(store, properties);
        RequestTracker tracker = new RequestTracker();
        IdempotencyKeyContext context = createContext();
        String payloadHash = "same-payload-hash";

        long elapsedMillis = executeRequests(arrivalPlanMillis, requestIndex -> {
            sleepQuietly(arrivalPlanMillis[requestIndex]);
            try {
                executor.execute(context, payloadHash, deterministicUpload(tracker));
            } catch (DomainException ex) {
                if (ex.getStatus() != ExceptionStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS) {
                    throw ex;
                }
            }
        });

        return BenchmarkResult.from("current", tracker, store.totalOps(), elapsedMillis);
    }

    private BenchmarkResult runWithOptimizedExecutor(long[] arrivalPlanMillis) throws Exception {
        CountingIdempotencyStore store = new CountingIdempotencyStore();
        IdempotencyProperties properties = createProperties();
        IdempotencyExecutor executor = new IdempotencyExecutor(store, properties);
        RequestTracker tracker = new RequestTracker();
        IdempotencyKeyContext context = createContext();
        String payloadHash = "same-payload-hash";

        long elapsedMillis = executeRequests(arrivalPlanMillis, requestIndex -> {
            sleepQuietly(arrivalPlanMillis[requestIndex]);
            try {
                executor.execute(context, payloadHash, deterministicUpload(tracker));
            } catch (DomainException ex) {
                if (ex.getStatus() != ExceptionStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS) {
                    throw ex;
                }
            }
        });

        return BenchmarkResult.from("optimized", tracker, store.totalOps(), elapsedMillis);
    }

    private long executeRequests(long[] arrivalPlanMillis, RequestWork requestWork) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        long startedAt = System.nanoTime();
        try (ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(TOTAL_REQUESTS);
            for (int requestIndex = 0; requestIndex < TOTAL_REQUESTS; requestIndex++) {
                final int currentRequestIndex = requestIndex;
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await();
                        requestWork.run(currentRequestIndex);
                    } catch (Throwable throwable) {
                        errors.add(throwable);
                    }
                }));
            }

            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }

        if (!errors.isEmpty()) {
            Throwable throwable = errors.peek();
            if (throwable instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(throwable);
        }

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private Supplier<String> deterministicUpload(RequestTracker tracker) {
        return () -> tracker.upload("idempotent-v1/1/bench-key/abcd1234.txt");
    }

    private IdempotencyProperties createProperties() {
        IdempotencyProperties properties = new IdempotencyProperties();
        properties.setProcessingStaleSeconds(60);
        properties.setRecordTtlSeconds(300);
        properties.setLockLeaseSeconds(3);
        return properties;
    }

    private IdempotencyKeyContext createContext() {
        return new IdempotencyKeyContext(1L, "study", "study:2:update", "bench-key");
    }

    private long[] createArrivalPlanMillis() {
        long[] arrivalPlanMillis = new long[TOTAL_REQUESTS];
        Random random = new Random(42L);

        arrivalPlanMillis[0] = 0L;
        for (int i = 1; i < 100; i++) {
            arrivalPlanMillis[i] = random.nextInt(3);
        }
        for (int i = 100; i < 400; i++) {
            arrivalPlanMillis[i] = 4L + random.nextInt(17);
        }
        for (int i = 400; i < TOTAL_REQUESTS; i++) {
            arrivalPlanMillis[i] = 40L + random.nextInt(211);
        }
        return arrivalPlanMillis;
    }

    private void sleepQuietly(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void printSummary(BenchmarkResult before, BenchmarkResult current, BenchmarkResult optimized) {
        System.out.printf("%n[duplicate-upload-benchmark] totalRequests=%d, uploadDurationMs=%d%n", TOTAL_REQUESTS, UPLOAD_DURATION_MILLIS);
        System.out.printf("%-10s | %16s | %16s | %18s | %18s | %10s%n",
                "scenario",
                "uploadExecs",
                "blockRate(%)",
                "duplicateFiles",
                "redisOps/request",
                "elapsedMs");
        printRow(before);
        printRow(current);
        printRow(optimized);
        System.out.println();
    }

    private void printRow(BenchmarkResult result) {
        System.out.printf("%-10s | %16d | %16.2f | %18d | %18.3f | %10d%n",
                result.scenario(),
                result.uploadExecutions(),
                result.blockedDuplicateRatePercent(),
                result.duplicateFileCount(),
                result.redisOpsPerRequest(),
                result.elapsedMillis());
    }

    private record BenchmarkResult(
            String scenario,
            long uploadExecutions,
            long duplicateFileCount,
            long redisOps,
            long elapsedMillis
    ) {
        private static BenchmarkResult from(String scenario, RequestTracker tracker, long redisOps, long elapsedMillis) {
            return new BenchmarkResult(
                    scenario,
                    tracker.uploadExecutions(),
                    tracker.duplicateFileCount(),
                    redisOps,
                    elapsedMillis
            );
        }

        private double blockedDuplicateRatePercent() {
            return ((TOTAL_REQUESTS - uploadExecutions) * 100.0d) / (TOTAL_REQUESTS - 1.0d);
        }

        private double redisOpsPerRequest() {
            return redisOps / (double) TOTAL_REQUESTS;
        }
    }

    private static class RequestTracker {

        private final LongAdder uploadExecutions = new LongAdder();
        private final Set<String> storedObjects = ConcurrentHashMap.newKeySet();

        private String upload(String objectKey) {
            uploadExecutions.increment();
            sleep(UPLOAD_DURATION_MILLIS);
            storedObjects.add(objectKey);
            return objectKey;
        }

        private long uploadExecutions() {
            return uploadExecutions.sum();
        }

        private long duplicateFileCount() {
            long uniqueObjectCount = storedObjects.size();
            return Math.max(0L, uniqueObjectCount - 1L);
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private static class CountingIdempotencyStore implements IdempotencyStore {

        private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final LongAdder findOps = new LongAdder();
        private final LongAdder saveOps = new LongAdder();
        private final LongAdder lockOps = new LongAdder();
        private final LongAdder unlockOps = new LongAdder();

        @Override
        public java.util.Optional<IdempotencyRecord> find(IdempotencyKeyContext context) {
            findOps.increment();
            return java.util.Optional.ofNullable(records.get(context.recordKey()));
        }

        @Override
        public void save(IdempotencyKeyContext context, IdempotencyRecord record, Duration ttl) {
            saveOps.increment();
            records.put(context.recordKey(), record);
        }

        @Override
        public boolean tryLock(IdempotencyKeyContext context, Duration waitTime, Duration leaseTime) {
            lockOps.increment();
            ReentrantLock lock = locks.computeIfAbsent(context.lockKey(), key -> new ReentrantLock());
            return lock.tryLock();
        }

        @Override
        public void unlock(IdempotencyKeyContext context) {
            unlockOps.increment();
            ReentrantLock lock = locks.computeIfAbsent(context.lockKey(), key -> new ReentrantLock());
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        private long totalOps() {
            return findOps.sum() + saveOps.sum() + lockOps.sum() + unlockOps.sum();
        }
    }

    private static class LegacyIdempotencyExecutor {

        private final IdempotencyStore idempotencyStore;
        private final IdempotencyProperties properties;

        private LegacyIdempotencyExecutor(IdempotencyStore idempotencyStore, IdempotencyProperties properties) {
            this.idempotencyStore = idempotencyStore;
            this.properties = properties;
        }

        private <T> T execute(IdempotencyKeyContext context, String payloadHash, Supplier<T> supplier) {
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
                Instant now = Instant.now();
                IdempotencyRecord existing = idempotencyStore.find(context).orElse(null);

                if (existing != null) {
                    validatePayloadHash(existing, payloadHash);

                    if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                        @SuppressWarnings("unchecked")
                        T replayed = (T) existing.getResponseData();
                        return replayed;
                    }

                    if (existing.getStatus() == IdempotencyStatus.PROCESSING
                            && !existing.isStale(now, properties.getProcessingStaleSeconds())) {
                        throw new DomainException(ExceptionStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS);
                    }
                }

                idempotencyStore.save(
                        context,
                        IdempotencyRecord.builder()
                                .status(IdempotencyStatus.PROCESSING)
                                .payloadHash(payloadHash)
                                .startedAt(now)
                                .updatedAt(now)
                                .build(),
                        Duration.ofSeconds(properties.getRecordTtlSeconds())
                );
                processingMarked = true;

                T result = supplier.get();
                idempotencyStore.save(
                        context,
                        IdempotencyRecord.builder()
                                .status(IdempotencyStatus.COMPLETED)
                                .payloadHash(payloadHash)
                                .startedAt(now)
                                .updatedAt(Instant.now())
                                .responseStatusCode(200)
                                .responseMessage("COMPLETED")
                                .responseData(result)
                                .build(),
                        Duration.ofSeconds(properties.getRecordTtlSeconds())
                );

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
            if (!java.util.Objects.equals(existing.getPayloadHash(), currentHash)) {
                throw new DomainException(ExceptionStatus.IDEMPOTENCY_PAYLOAD_MISMATCH);
            }
        }
    }

    @FunctionalInterface
    private interface RequestWork {
        void run(int requestIndex);
    }
}
