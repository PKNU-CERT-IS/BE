package org.certis.studyplatform.shared.idempotency;

public record IdempotencyKeyContext(
        Long memberId,
        String route,
        String resource,
        String idempotencyKey
) {
    public String recordKey() {
        return "idem:req:%d:%s:%s:%s".formatted(memberId, route, resource, idempotencyKey);
    }

    public String lockKey() {
        return "idem:lock:%d:%s:%s:%s".formatted(memberId, route, resource, idempotencyKey);
    }
}
