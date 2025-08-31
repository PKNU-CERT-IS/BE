package org.certis.studyplatform.project.application.object.query;

import org.springframework.data.domain.Pageable;

/**
 * Get All Project Meetings Query
 *
 * 프로젝트 회의록 전체 목록 조회 쿼리 객체
 */
public record GetAllProjectMeetingsQuery(
    Long projectId,
    Pageable pageable
) {
    public static GetAllProjectMeetingsQuery of(Long projectId, Pageable pageable) {
        return new GetAllProjectMeetingsQuery(projectId, pageable);
    }
} 