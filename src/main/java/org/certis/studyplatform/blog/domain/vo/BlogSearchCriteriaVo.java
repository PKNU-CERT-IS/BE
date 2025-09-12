package org.certis.studyplatform.blog.domain.vo;

/**
 * Blog Search Criteria Value Object
 *
 * 블로그 검색 조건을 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record BlogSearchCriteriaVo(
        String keyword,
        String category
) {
    /**
     * 기본 생성자
     */
    public static BlogSearchCriteriaVo of(
            String keyword,
            String category) {
        return new BlogSearchCriteriaVo(keyword, category);
    }

    /**
     * 빈 검색 조건 생성
     */
    public static BlogSearchCriteriaVo empty() {
        return new BlogSearchCriteriaVo(null, null);
    }

    /**
     * 키워드만으로 검색 조건 생성
     */
    public static BlogSearchCriteriaVo ofKeyword(String keyword) {
        return new BlogSearchCriteriaVo(keyword, null);
    }

    /**
     * 카테고리만으로 검색 조건 생성
     */
    public static BlogSearchCriteriaVo ofCategory(String category) {
        return new BlogSearchCriteriaVo(null, category);
    }

    /**
     * 키워드가 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 카테고리가 있는지 확인
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return !hasKeyword() && !hasCategory();
    }
}