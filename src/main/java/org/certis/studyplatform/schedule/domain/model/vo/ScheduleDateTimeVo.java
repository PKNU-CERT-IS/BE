package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.time.OffsetDateTime;

public record ScheduleDateTimeVo(OffsetDateTime startedAt, OffsetDateTime endedAt) {

    public static ScheduleDateTimeVo of(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        return new ScheduleDateTimeVo(startedAt, endedAt);
    }

    public ScheduleDateTimeVo {
        validateNotNull(startedAt, endedAt);
        validateTimeOrder(startedAt, endedAt);
        validateFutureTime(startedAt);
    }

    // 값이 null 값이 아닌지 판별
    private void validateNotNull(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TIME);
        }
    }

    // 끝나는 시간이 시작 시간 보다 앞서면 안됨
    private void validateTimeOrder(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        if (!startedAt.isBefore(endedAt)) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TIME_ORDER);
        }
    }

    // 시작시간이 지금보다 더 과거의 시간이면 안됨
    private void validateFutureTime(OffsetDateTime startedAt) {
        if (startedAt.isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_PAST_TIME);
        }
    }
}
