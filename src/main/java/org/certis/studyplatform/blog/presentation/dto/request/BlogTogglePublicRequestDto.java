package org.certis.studyplatform.blog.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Blog Toggle Public Request DTO
 *
 * Admin용 블로그 공개 유무 토글 요청 데이터
 */
@Getter
@Setter
public class BlogTogglePublicRequestDto {

    @NotNull(message = "블로그 ID는 필수입니다")
    @Positive(message = "블로그 ID는 양수여야 합니다")
    private Long blogId;

    @NotNull(message = "공개 유무는 필수입니다")
    private Boolean isPublic;

}
