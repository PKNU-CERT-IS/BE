package org.certis.studyplatform.shared.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private IdempotencyStatus status;
    private String payloadHash;
    private Instant startedAt;
    private Instant updatedAt;
    private Integer responseStatusCode;
    private String responseMessage;
    private Object responseData;

    public boolean isStale(Instant now, long staleSeconds) {
        if (startedAt == null) {
            return true;
        }
        return startedAt.plusSeconds(staleSeconds).isBefore(now);
    }
}
