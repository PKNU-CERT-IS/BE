package org.certis.studyplatform.project.application.object.command;

import java.util.List;

/**
 * Update Project Meeting Command
 *
 * 프로젝트 회의록 수정 명령 객체
 */
public record UpdateProjectMeetingCommand(
    Long meetingId,
    Long requesterId,
    String title,
    String content,
    List<Long> participantIds,
    String attachedUrl
) {
    public static UpdateProjectMeetingCommand of(
        Long meetingId,
        Long requesterId,
        String title,
        String content,
        List<Long> participantIds,
        String attachedUrl
    ) {
        return new UpdateProjectMeetingCommand(
            meetingId, requesterId, title, content, participantIds, attachedUrl
        );
    }
} 