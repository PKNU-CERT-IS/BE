package org.certis.studyplatform.study.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class StudyJoinResponseDto {

    private Long participantId;
    private Long studyId;
    private StudyParticipantStatus status;
    private OffsetDateTime createdAt;
}
