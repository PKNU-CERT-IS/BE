package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleTitleVo(String value) {

    public static ScheduleTitleVo of(String title) {
        return new ScheduleTitleVo(title);
    }

    public ScheduleTitleVo {
        validateNotBlank(value);
        validateLength(value);
    }

    private void validateNotBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TITLE);
        }
    }

    private void validateLength(String value) {
        if (value.trim().length() > 20) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TITLE);
        }
    }
}

