package org.certis.studyplatform.study.application.object.query;

/**
 * Get Study Meeting By ID Query
 *
 * 스터디 회의록 ID로 상세 조회하는 쿼리 객체
 */
public record GetStudyMeetingByIdQuery(Long meetingId) {
    public static GetStudyMeetingByIdQuery of(Long meetingId) {
        return new GetStudyMeetingByIdQuery(meetingId);
    }
} 