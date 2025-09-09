package org.certis.studyplatform.study.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Study Participant Stats Response DTO
 *
 * 프로젝트 참가자 통계 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
public class StudyParticipantStatsResponseDto {

    private Long studyId;
    private Long approvedCount;
    private Long pendingCount;
    private Integer maxParticipants;
    private Boolean isFull;
}
