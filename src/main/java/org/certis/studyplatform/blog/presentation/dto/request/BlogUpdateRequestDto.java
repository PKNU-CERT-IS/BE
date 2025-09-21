package org.certis.studyplatform.blog.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
/**
 * Blog Update Request DTO
 *
 * 블로그 수정 요청 데이터
 */
@Getter
@Setter
public class BlogUpdateRequestDto {

    @NotNull(message = "블로그 ID는 필수입니다")
    @Positive(message = "블로그 ID는 양수여야 합니다")
    private Long blogId;

    private String title;

    private String description;

    private String content;

    private String category;

    private ArticleReferenceType referenceType;

    private Long referenceId;

    private String referenceTitle;

    private Boolean isPublic;
}