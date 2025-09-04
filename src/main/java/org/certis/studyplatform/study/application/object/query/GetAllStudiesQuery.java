package org.certis.studyplatform.study.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Get All Studies Query
 *
 * 전체 스터디 조회 쿼리 객체
 */
public record GetAllStudiesQuery(Pageable pageable) {
    public static GetAllStudiesQuery of(Pageable pageable) {
        return new GetAllStudiesQuery(pageable);
    }
}