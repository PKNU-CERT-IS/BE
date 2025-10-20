package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * External URL Request DTO
 *
 * 외부 URL 정보를 나타내는 요청 DTO
 */
@Getter
@Setter
public class ExternalUrlRequestDto {

    @NotBlank(message = "외부 URL 제목은 필수입니다")
    private String title;

    @NotBlank(message = "외부 URL은 필수입니다")
    private String url;
}
