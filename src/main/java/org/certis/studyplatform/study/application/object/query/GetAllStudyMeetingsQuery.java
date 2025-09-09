package org.certis.studyplatform.study.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Get All Study Meetings Query
 *
 * 스터디 회의록 전체 목록 조회 쿼리 객체
 */
public record GetAllStudyMeetingsQuery(
    Long studyId,
    Pageable pageable
) {
    public static GetAllStudyMeetingsQuery of(Long studyId, Pageable pageable) {
        return new GetAllStudyMeetingsQuery(studyId, pageable);
    }
} 