package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

/**
 * Project End Request DTO
 *
 * 프로젝트 종료 요청을 위한 DTO
 * Presentation Layer
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectEndRequestDto {
    
    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;
    @NotBlank(message = "종료 신청 보고서 URL은 필수입니다")
    private String attachmentUrl;
    
    public ProjectEndRequestDto(Long projectId, String attachmentUrl) {
        this.projectId = projectId;
        this.attachmentUrl = attachmentUrl;
    }
}