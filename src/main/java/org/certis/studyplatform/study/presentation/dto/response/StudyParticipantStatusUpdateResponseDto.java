package org.certis.studyplatform.study.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class StudyParticipantStatusUpdateResponseDto {

    private Long studyId;
    private Long memberId;
    private StudyParticipantStatus previousStatus;
    private StudyParticipantStatus currentStatus;
    private String message;
    private OffsetDateTime updatedAt;
}