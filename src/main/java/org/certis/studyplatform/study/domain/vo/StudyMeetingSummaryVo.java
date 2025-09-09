package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;

/**
 * Study Meeting Summary Value Object
 *
 * 스터디 회의록 요약 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record StudyMeetingSummaryVo(
    Long id,
    String title,
    Integer participantNumber,
    String creatorName,
    boolean isEditable,
    OffsetDateTime createdAt,
    String meetingAttachedUrl,
    String meetingAttachedTitle
) {
    /**
     * 기본 생성자
     */
    public static StudyMeetingSummaryVo of(
            Long id,
            String title,
            Integer participantNumber,
            String creatorName,
            boolean isEditable,
            OffsetDateTime createdAt,
            String meetingAttachedUrl,
            String meetingAttachedTitle) {
        return new StudyMeetingSummaryVo(id, title, participantNumber, creatorName, isEditable,
                createdAt, meetingAttachedUrl, meetingAttachedTitle);
    }
} 