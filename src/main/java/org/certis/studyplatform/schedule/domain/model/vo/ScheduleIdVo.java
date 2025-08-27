package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleIdVo(Long value) {

    public static ScheduleIdVo of(Long id) {
        return new ScheduleIdVo(id);
    }

    public ScheduleIdVo {
        validateNotNull(value);
        validatePositive(value);
    }
    private void validateNotNull(Long value) {
        if (value == null) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_ID);
        }
    }

    private void validatePositive(Long value) {
        if (value <= 0) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_ID);
        }
    }
}