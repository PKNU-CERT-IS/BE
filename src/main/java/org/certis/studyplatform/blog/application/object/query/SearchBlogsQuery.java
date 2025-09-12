package org.certis.studyplatform.blog.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Search Blogs Query
 *
 * 블로그 검색 쿼리 객체
 * 고급 검색 필터 지원 (keyword,category)
 */
public record SearchBlogsQuery(
    String keyword,
    String category,
    Pageable pageable
) {
    public static SearchBlogsQuery of(
        String keyword,
        String category,
        Pageable pageable
    ) {
        return new SearchBlogsQuery(keyword, category, pageable);
    }

    /**
     * 고급 검색용 팩토리 메서드
     */
    public static SearchBlogsQuery ofAdvanced(
        String keyword,
        String category,
        Pageable pageable
    ) {
        return new SearchBlogsQuery(keyword, category, pageable);
    }

    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty()) ;
    }

    /**
     * 키워드 검색이 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 카테고리 필터가 있는지 확인
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }
}