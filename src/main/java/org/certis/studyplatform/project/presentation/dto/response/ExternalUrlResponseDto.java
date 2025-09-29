package org.certis.studyplatform.project.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * External URL Response DTO
 *
 * 외부 URL 정보를 나타내는 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUrlResponseDto {

    private String title;
    private String url;
}
