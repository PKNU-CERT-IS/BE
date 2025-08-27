package org.certis.studyplatform.schedule.application.object.command;

import java.time.OffsetDateTime;

public record CreateScheduleByAdminCommand(
        Long adminId,
        String title,
        String description,
        String type,
        String place,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
    public static CreateScheduleByAdminCommand of(
            Long adminId,
            String title,
            String description,
            String type,
            String place,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt
    ){
        return new CreateScheduleByAdminCommand(
                adminId, title, description, type, place, startedAt, endedAt);
    }
}
