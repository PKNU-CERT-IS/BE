package org.certis.studyplatform.study.application.object.command;

import java.util.List;
import org.certis.studyplatform.shared.dto.LinkDto;

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
    Integer participantNumber,
    List<LinkDto> links
) {
    public static CreateStudyMeetingCommand of(
        Long studyId,
        Long writerId,
        String title,
        String content,
        Integer participantNumber,
        List<LinkDto> links
    ) {
        return new CreateStudyMeetingCommand(
            studyId, writerId, title, content, participantNumber, links
        );
    }
} 