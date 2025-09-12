package org.certis.studyplatform.study.application.object.command;

import java.util.List;

/**
 * Create Study Meeting Command
 *
 * 스터디 회의록 생성 명령 객체
 */
public record CreateStudyMeetingCommand(
    Long studyId,
    Long writerId,
    String title,
    String content,
    List<Long> participantIds,
    String attachedUrl
) {
    public static CreateStudyMeetingCommand of(
        Long studyId,
        Long writerId,
        String title,
        String content,
        List<Long> participantIds,
        String attachedUrl
    ) {
        return new CreateStudyMeetingCommand(
            studyId, writerId, title, content, participantIds, attachedUrl
        );
    }
} 