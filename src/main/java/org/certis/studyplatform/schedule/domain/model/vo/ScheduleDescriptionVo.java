package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleDescriptionVo(String value) {

    public static ScheduleDescriptionVo of(String description) {
        return new ScheduleDescriptionVo(description);
    }

    public ScheduleDescriptionVo {
        validateLength(value);
    }

    private void validateLength(String value) {
        if (value != null && value.length() > 50) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_DESCRIPTION);
        }
    }
}
