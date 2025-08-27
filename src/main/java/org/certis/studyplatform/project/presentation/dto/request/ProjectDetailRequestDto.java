package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Project Detail Request DTO
 *
 * 프로젝트 상세 조회 요청 데이터
 * @ModelAttribute로 사용되는 GET 파라미터 DTO
 */
@Getter
@Setter
public class ProjectDetailRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Positive(message = "프로젝트 ID는 양수여야 합니다")
    private Long projectId;
}