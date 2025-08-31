package org.certis.studyplatform.project.application.object.query;

/**
 * Get Project By ID Query
 *
 * 프로젝트 ID로 조회하는 쿼리 객체
 */
public record GetProjectByIdQuery(Long id) {
    public static GetProjectByIdQuery of(Long id) {
        return new GetProjectByIdQuery(id);
    }
}