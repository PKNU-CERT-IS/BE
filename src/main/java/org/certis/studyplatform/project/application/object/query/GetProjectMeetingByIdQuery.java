package org.certis.studyplatform.project.application.object.query;

/**
 * Get Project Meeting By ID Query
 *
 * 프로젝트 회의록 ID로 상세 조회하는 쿼리 객체
 */
public record GetProjectMeetingByIdQuery(Long meetingId) {
    public static GetProjectMeetingByIdQuery of(Long meetingId) {
        return new GetProjectMeetingByIdQuery(meetingId);
    }
} 