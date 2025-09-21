package org.certis.studyplatform.shared.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Link DTO
 *
 * 링크 정보를 나타내는 공용 DTO
 */
@Getter
@Setter
public class LinkDto {

    @NotBlank(message = "링크 제목은 필수입니다")
    private String title;

    @NotBlank(message = "링크 URL은 필수입니다")
    private String url;

    public LinkDto() {}

    public LinkDto(String title, String url) {
        this.title = title;
        this.url = url;
    }
}
