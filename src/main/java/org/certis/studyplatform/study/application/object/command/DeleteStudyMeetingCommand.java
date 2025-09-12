package org.certis.studyplatform.study.application.object.command;

/**
 * Delete Study Meeting Command
 *
 * 스터디 회의록 삭제 명령 객체
 */
public record DeleteStudyMeetingCommand(
    Long meetingId,
    Long requesterId
) {
    public static DeleteStudyMeetingCommand of(Long meetingId, Long requesterId) {
        return new DeleteStudyMeetingCommand(meetingId, requesterId);
    }
} 