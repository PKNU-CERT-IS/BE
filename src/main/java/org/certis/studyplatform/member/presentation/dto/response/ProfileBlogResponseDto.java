package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.project.domain.ProjectStatus;

import java.time.OffsetDateTime;

/**
 * 프로필용 블로그 응답 DTO
 * 내가 작성한 블로그 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileBlogResponseDto {

    private Long blogId;
    private String title;
    private String description;
    private ProjectStatus projectStatus;
    private OffsetDateTime blogStartDate;
    private OffsetDateTime blogEndDate;
    private String[] tags;
    private Integer viewCount;
    private Integer likeCount;
    
    // Category information (blogs only have category, no subcategory)
    private String category;
    
    // Reference information
    private ArticleReferenceType referenceType;
    private String referenceTitle;
}