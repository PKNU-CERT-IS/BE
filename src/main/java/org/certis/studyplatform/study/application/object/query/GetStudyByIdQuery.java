package org.certis.studyplatform.study.application.object.query;

/**
 * Get Study By ID Query
 *
 * 스터디 ID로 조회하는 쿼리 객체
 */
public record GetStudyByIdQuery(Long id) {
    public static GetStudyByIdQuery of(Long id) {
        return new GetStudyByIdQuery(id);
    }
}