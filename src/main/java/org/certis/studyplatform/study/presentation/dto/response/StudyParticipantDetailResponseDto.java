package org.certis.studyplatform.study.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class StudyParticipantDetailResponseDto {

    private Long id;
    private Long studyId;
    private String studyTitle;
    private Long memberId;
    private String memberName;
    private StudyParticipantStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}