package org.certis.studyplatform.study.application.object.command;

import java.util.List;

/**
 * Update Study Meeting Command
 *
 * 스터디 회의록 수정 명령 객체
 */
public record UpdateStudyMeetingCommand(
    Long meetingId,
    Long requesterId,
    String title,
    String content,
    List<Long> participantIds,
    String attachedUrl
) {
    public static UpdateStudyMeetingCommand of(
        Long meetingId,
        Long requesterId,
        String title,
        String content,
        List<Long> participantIds,
        String attachedUrl
    ) {
        return new UpdateStudyMeetingCommand(
            meetingId, requesterId, title, content, participantIds, attachedUrl
        );
    }
} 