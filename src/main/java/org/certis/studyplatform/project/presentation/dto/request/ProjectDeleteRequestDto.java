package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Project Delete Request DTO
 *
 * 프로젝트 삭제 요청 데이터
 */
@Getter
@Setter
public class ProjectDeleteRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Positive(message = "프로젝트 ID는 양수여야 합니다")
    private Long projectId;

    @NotNull(message = "요청자 ID는 필수입니다")
    @Positive(message = "요청자 ID는 양수여야 합니다")
    private Long requesterId;
}