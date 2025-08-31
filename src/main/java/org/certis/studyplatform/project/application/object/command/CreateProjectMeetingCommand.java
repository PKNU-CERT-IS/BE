package org.certis.studyplatform.project.application.object.command;

import java.util.List;

/**
 * Create Project Meeting Command
 *
 * 프로젝트 회의록 생성 명령 객체
 */
public record CreateProjectMeetingCommand(
    Long projectId,
    Long writerId,
    String title,
    String content,
    List<Long> participantIds,
    String attachedUrl
) {
    public static CreateProjectMeetingCommand of(
        Long projectId,
        Long writerId,
        String title,
        String content,
        List<Long> participantIds,
        String attachedUrl
    ) {
        return new CreateProjectMeetingCommand(
            projectId, writerId, title, content, participantIds, attachedUrl
        );
    }
} 