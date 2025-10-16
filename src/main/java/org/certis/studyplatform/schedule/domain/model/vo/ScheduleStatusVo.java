package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.schedule.domain.model.ScheduleStatus;

public record ScheduleStatusVo(ScheduleStatus status) {

    public static ScheduleStatusVo of(String status) {
        return new ScheduleStatusVo(ScheduleStatus.isScheduleStatus(status));
    }

    public static ScheduleStatusVo pending() {
        return new ScheduleStatusVo(ScheduleStatus.PENDING);
    }

    public static ScheduleStatusVo approved() {
        return new ScheduleStatusVo(ScheduleStatus.APPROVED);
    }

    public static ScheduleStatusVo rejected() {
        return new ScheduleStatusVo(ScheduleStatus.REJECTED);
    }

    public boolean isPending() {
        return status == ScheduleStatus.PENDING;
    }

    public boolean isApproved() {
        return status == ScheduleStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == ScheduleStatus.REJECTED;
    }

    public String value(){
        return status.getValue();
    }
}
