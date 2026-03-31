package org.certis.studyplatform.shared.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(IdempotencyKeyContext context);

    void save(IdempotencyKeyContext context, IdempotencyRecord record, Duration ttl);

    boolean tryLock(IdempotencyKeyContext context, Duration waitTime, Duration leaseTime);

    void unlock(IdempotencyKeyContext context);
}
