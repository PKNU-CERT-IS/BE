package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Project Meeting Delete Request DTO
 *
 * 프로젝트 회의록 삭제 요청 데이터
 */
@Getter
@Setter
public class ProjectMeetingDeleteRequestDto {

    @NotNull(message = "회의록 ID는 필수입니다")
    @Positive(message = "회의록 ID는 양수여야 합니다")
    private Long meetingId;

    @NotNull(message = "요청자 ID는 필수입니다")
    @Positive(message = "요청자 ID는 양수여야 합니다")
    private Long requesterId;
} 