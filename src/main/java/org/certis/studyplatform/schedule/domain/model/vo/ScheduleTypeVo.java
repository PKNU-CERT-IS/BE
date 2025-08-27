package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleTypeVo(String value) {

    private static final String INFORMATION = "INFORMATION";
    private static final String ADVERTISE = "ADVERTISE";

    public static ScheduleTypeVo of(String type) {
        return new ScheduleTypeVo(type);
    }

    public ScheduleTypeVo {
        validateType(value);
    }

    private void validateType(String value) {
        if ((!INFORMATION.equals(value) && !ADVERTISE.equals(value))) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TYPE);
        }
    }
}