package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

/**
 * Study End Request DTO
 *
 * 스터디 종료 요청을 위한 DTO
 * Presentation Layer
 */
@Getter
@Setter
@NoArgsConstructor
public class StudyEndRequestDto {
    
    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;
    
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