package org.certis.studyplatform.blog.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Get All Blogs Query
 *
 * 전체 프로젝트 조회 쿼리 객체
 */
public record GetAllBlogsQuery(Pageable pageable) {
    public static GetAllBlogsQuery of(Pageable pageable) {
        return new GetAllBlogsQuery(pageable);
    }
}