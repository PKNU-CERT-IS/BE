package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Study Meeting Delete Request DTO
 *
 * 스터디 회의록 삭제 요청 데이터
 */
@Getter
@Setter
public class StudyMeetingDeleteRequestDto {

    @NotNull(message = "회의록 ID는 필수입니다")
    @Positive(message = "회의록 ID는 양수여야 합니다")
    private Long meetingId;
} 