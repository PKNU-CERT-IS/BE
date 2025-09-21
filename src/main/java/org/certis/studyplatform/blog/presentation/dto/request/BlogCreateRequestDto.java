package org.certis.studyplatform.blog.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;

/**
 * Blog Create Request DTO
 *
 * 블로그 생성 요청 데이터
 */
@Getter
@Setter
public class BlogCreateRequestDto {

    @NotBlank(message = "블로그 제목은 필수입니다")
    private String title;

    @NotBlank(message = "블로그 설명은 필수입니다")
    private String description;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @NotNull(message = "내용은 필수입니다")
    private String content;

    @NotNull(message = "작성하고자 하는 종류 설정은 필수입니다")
    private ArticleReferenceType referenceType;

    @NotNull(message = "작성하고자 하는 종류 ID는 필수입니다")
    private Long referenceId;

    private String referenceTitle;

    private Boolean isPublic = true; // 기본값은 공개

}