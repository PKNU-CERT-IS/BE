package org.certis.studyplatform.project.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Get All Projects Query
 *
 * 전체 프로젝트 조회 쿼리 객체
 */
public record GetAllProjectsQuery(Pageable pageable) {
    public static GetAllProjectsQuery of(Pageable pageable) {
        return new GetAllProjectsQuery(pageable);
    }
}