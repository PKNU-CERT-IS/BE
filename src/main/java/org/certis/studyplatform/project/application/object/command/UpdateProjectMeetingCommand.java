package org.certis.studyplatform.project.application.object.command;

import java.util.List;
import org.certis.studyplatform.shared.dto.LinkDto;

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
    Integer participantNumber,
    List<LinkDto> links
) {
    public static UpdateProjectMeetingCommand of(
        Long meetingId,
        Long requesterId,
        String title,
        String content,
        Integer participantNumber,
        List<LinkDto> links
    ) {
        return new UpdateProjectMeetingCommand(
            meetingId, requesterId, title, content, participantNumber, links
        );
    }
} 