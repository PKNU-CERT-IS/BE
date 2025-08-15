package org.certis.studyplatform.schedule.application.object.command;

public record DeleteClubRoomUsageCommand(
        Long memberId,
        Long scheduleId
) { public static DeleteClubRoomUsageCommand of(
        Long memberId,
        Long scheduleId
) {
    return new DeleteClubRoomUsageCommand(memberId, scheduleId);}
}
