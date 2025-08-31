package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Project Join Request DTO
 *
 * 프로젝트 참가 신청 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectJoinRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;
}