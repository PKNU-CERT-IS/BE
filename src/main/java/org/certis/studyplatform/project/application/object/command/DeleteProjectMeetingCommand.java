package org.certis.studyplatform.project.application.object.command;

/**
 * Delete Project Meeting Command
 *
 * 프로젝트 회의록 삭제 명령 객체
 */
public record DeleteProjectMeetingCommand(
    Long meetingId,
    Long requesterId
) {
    public static DeleteProjectMeetingCommand of(Long meetingId, Long requesterId) {
        return new DeleteProjectMeetingCommand(meetingId, requesterId);
    }
} 