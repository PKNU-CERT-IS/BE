package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleStatusVo(String value) {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    public static ScheduleStatusVo of(String status) {
        return new ScheduleStatusVo(status);
    }

    public static ScheduleStatusVo pending() {
        return new ScheduleStatusVo(PENDING);
    }

    public static ScheduleStatusVo approved() {
        return new ScheduleStatusVo(APPROVED);
    }

    public static ScheduleStatusVo rejected() {
        return new ScheduleStatusVo(REJECTED);
    }

    public ScheduleStatusVo {
        validateStatus(value);
    }

    private void validateStatus(String value) {
        if ((!PENDING.equals(value) && !APPROVED.equals(value) && !REJECTED.equals(value))) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_STATUS);
        }
    }

    public boolean isPending() {
        return PENDING.equals(value);
    }

    public boolean isApproved() {
        return APPROVED.equals(value);
    }

    public boolean isRejected() {
        return REJECTED.equals(value);
    }
}
