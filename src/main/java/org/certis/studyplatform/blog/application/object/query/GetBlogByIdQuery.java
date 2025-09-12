package org.certis.studyplatform.blog.application.object.query;

/**
 * Get Blog By ID Query
 *
 * 블로그 단건 조회 쿼리 객체
 * viewerId는 조회수 증가 및 중복 조회 방지에 사용
 */
public record GetBlogByIdQuery(
        Long id,
        Long viewerId  // 조회자 ID (조회수 증가 및 중복 방지용)
) {
    public static GetBlogByIdQuery of(Long id, Long viewerId) {
        return new GetBlogByIdQuery(id, viewerId);
    }

    /**
     * viewerId 없이 조회 (관리자용 또는 조회수 증가 없이 조회)
     */
    public static GetBlogByIdQuery ofWithoutViewer(Long id) {
        return new GetBlogByIdQuery(id, null);
    }
}