package org.certis.studyplatform.project.application.object.command;

import java.util.List;
import org.certis.studyplatform.shared.dto.LinkDto;

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
    Integer participantNumber,
    List<LinkDto> links
) {
    public static CreateProjectMeetingCommand of(
        Long projectId,
        Long writerId,
        String title,
        String content,
        Integer participantNumber,
        List<LinkDto> links
    ) {
        return new CreateProjectMeetingCommand(
            projectId, writerId, title, content, participantNumber, links
        );
    }
} 