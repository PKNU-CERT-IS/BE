package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.Valid;
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
    
    @NotNull(message = "첨부 객체는 필수입니다")
    @Valid
    private Attachment attachment;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Attachment {
        private Long id;
        private String name;
        private String type;
        private String size;
        @NotBlank(message = "첨부 URL은 필수입니다")
        private String attachedUrl;
    }
}