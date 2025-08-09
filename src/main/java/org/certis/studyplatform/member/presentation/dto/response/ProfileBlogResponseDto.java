package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

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
    private String summary; // 요약 또는 첫 몇 줄
    private String status; // DRAFT, PUBLISHED, PRIVATE
    private ZonedDateTime createdAt;
    private ZonedDateTime publishedAt;
    private ZonedDateTime updatedAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private List<String> categories;
    private List<String> tags;
    private String thumbnailUrl;
}