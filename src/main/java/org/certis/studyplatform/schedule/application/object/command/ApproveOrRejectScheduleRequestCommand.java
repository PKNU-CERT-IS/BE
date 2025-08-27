package org.certis.studyplatform.schedule.application.object.command;

public record ApproveOrRejectScheduleRequestCommand(
        Long adminId,
        Long scheduleId,
        String status
) {
    public static ApproveOrRejectScheduleRequestCommand of( Long adminId,
                                                            Long scheduleId,
                                                            String status){
        return new ApproveOrRejectScheduleRequestCommand(adminId, scheduleId, status);
    }
}
