package org.certis.studyplatform.schedule.application.object.command;

import java.time.OffsetDateTime;

public record CreateClubRoomUsageCommand(
        Long memberId,
        String title,
        String description,
        String type,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt)
{
    public static CreateClubRoomUsageCommand of(
            Long memberId,
            String title,
            String description,
            String type,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt
    ){
        return new CreateClubRoomUsageCommand(
                memberId,
                title,
                description,
                type,
                startedAt,
                endedAt);
    }
}
