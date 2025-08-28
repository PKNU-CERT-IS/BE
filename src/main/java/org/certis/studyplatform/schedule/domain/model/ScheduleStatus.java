package org.certis.studyplatform.schedule.domain.model;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public enum ScheduleStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String value;

    ScheduleStatus(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }


    public static ScheduleStatus isScheduleStatus(String value) {
        for (ScheduleStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TYPE,"적절하지 않은 ScheduleStatus 타입입니다.");
    }
}
