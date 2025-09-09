package org.certis.studyplatform.blog.domain.vo;

import java.util.List;

/**
 * Blog Search Result Value Object
 *
 * 블로그 검색 결과를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record BlogSearchResultVo(
        List<BlogSummaryVo> blogs,
        long totalElements
) {
    /**
     * 기본 생성자
     */
    public static BlogSearchResultVo of(
            List<BlogSummaryVo> blogs,
            long totalElements) {
        return new BlogSearchResultVo(blogs, totalElements);
    }

    /**
     * 빈 결과 생성
     */
    public static BlogSearchResultVo empty() {
        return new BlogSearchResultVo(List.of(), 0L);
    }
}