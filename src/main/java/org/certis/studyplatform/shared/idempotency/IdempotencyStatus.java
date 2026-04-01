package org.certis.studyplatform.shared.idempotency;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED_RETRYABLE,
    FAILED_FINAL
}
