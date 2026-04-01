package org.certis.studyplatform.shared.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyStore implements IdempotencyStore {

    private final RedissonClient redissonClient;

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKeyContext context) {
        RBucket<IdempotencyRecord> bucket = redissonClient.getBucket(context.recordKey());
        return Optional.ofNullable(bucket.get());
    }

    @Override
    public void save(IdempotencyKeyContext context, IdempotencyRecord record, Duration ttl) {
        RBucket<IdempotencyRecord> bucket = redissonClient.getBucket(context.recordKey());
        bucket.set(record, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean tryLock(IdempotencyKeyContext context, Duration waitTime, Duration leaseTime) {
        RLock lock = redissonClient.getLock(context.lockKey());
        try {
            return lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting idempotency lock: {}", context.lockKey());
            return false;
        }
    }

    @Override
    public void unlock(IdempotencyKeyContext context) {
        RLock lock = redissonClient.getLock(context.lockKey());
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
