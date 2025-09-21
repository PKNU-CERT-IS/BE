package org.certis.studyplatform.study.application.object.command;

import java.util.List;
import org.certis.studyplatform.shared.dto.LinkDto;

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
    Integer participantNumber,
    List<LinkDto> links
) {
    public static UpdateStudyMeetingCommand of(
        Long meetingId,
        Long requesterId,
        String title,
        String content,
        Integer participantNumber,
        List<LinkDto> links
    ) {
        return new UpdateStudyMeetingCommand(
            meetingId, requesterId, title, content, participantNumber, links
        );
    }
} 