package org.certis.studyplatform.schedule.application.object.command;

public record DeleteScheduleCommand(Long adminId,
                                    Long scheduleId) {
    public  static DeleteScheduleCommand of(Long adminId,
                                            Long scheduleId) {
        return new DeleteScheduleCommand(adminId, scheduleId);
    }
}
