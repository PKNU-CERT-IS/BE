package org.certis.studyplatform.member.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 블로그 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record ProfileBlogVo(
        Long blogId,
        String title,
        String summary, // 요약 또는 첫 몇 줄
        String status, // DRAFT, PUBLISHED, PRIVATE
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        List<String> categories,
        List<String> tags,
        String thumbnailUrl
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileBlogVo {
        // List들의 불변성 보장
        categories = categories != null ? List.copyOf(categories) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();

        // 카운트 필드들의 기본값 설정
        viewCount = viewCount != null ? viewCount : 0;
        likeCount = likeCount != null ? likeCount : 0;
        commentCount = commentCount != null ? commentCount : 0;
    }

    /**
     * 편의 생성자 - 최소 필드만
     */
    public ProfileBlogVo(Long blogId, String title, String summary, String status,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(blogId, title, summary, status, createdAt, null, updatedAt,
                0, 0, 0, List.of(), List.of(), null);
    }

    /**
     * 발행된 블로그인지 확인
     */
    public boolean isPublished() {
        return "PUBLISHED".equals(status);
    }

    /**
     * 임시저장 상태인지 확인
     */
    public boolean isDraft() {
        return "DRAFT".equals(status);
    }

    /**
     * 비공개 블로그인지 확인
     */
    public boolean isPrivate() {
        return "PRIVATE".equals(status);
    }

    /**
     * 카테고리 배열 반환 (Presentation Layer 호환)
     */
    public String[] getCategoriesArray() {
        return categories.toArray(new String[0]);
    }

    /**
     * 태그 배열 반환 (Presentation Layer 호환)
     */
    public String[] getTagsArray() {
        return tags.toArray(new String[0]);
    }

    /**
     * 최근에 업데이트되었는지 확인 (7일 이내)
     */
    public boolean isRecentlyUpdated() {
        if (updatedAt == null) return false;
        OffsetDateTime weekAgo = OffsetDateTime.now().minusDays(7);
        return updatedAt.isAfter(weekAgo);
    }

    /**
     * 최근에 발행되었는지 확인 (30일 이내)
     */
    public boolean isRecentlyPublished() {
        if (publishedAt == null) return false;
        OffsetDateTime monthAgo = OffsetDateTime.now().minusDays(30);
        return publishedAt.isAfter(monthAgo);
    }

    /**
     * 인기 블로그인지 확인 (조회수 기준)
     */
    public boolean isPopular() {
        return viewCount >= 100;
    }

    /**
     * 높은 참여도를 가진 블로그인지 확인 (좋아요 + 댓글)
     */
    public boolean isHighEngagement() {
        return (likeCount + commentCount) >= 20;
    }

    /**
     * 썸네일이 있는지 확인
     */
    public boolean hasThumbnail() {
        return thumbnailUrl != null && !thumbnailUrl.trim().isEmpty();
    }

    /**
     * 특정 카테고리에 속하는지 확인
     */
    public boolean belongsToCategory(String category) {
        return categories.stream()
                .anyMatch(cat -> cat.equalsIgnoreCase(category));
    }

    /**
     * 특정 태그를 가지고 있는지 확인
     */
    public boolean hasTag(String tag) {
        return tags.stream()
                .anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    /**
     * 총 상호작용 수 반환 (조회수 + 좋아요 + 댓글)
     */
    public int getTotalInteractions() {
        return viewCount + likeCount + commentCount;
    }
}