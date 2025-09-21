package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;

/**
 * Project Meeting Summary Value Object
 *
 * 프로젝트 회의록 요약 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record ProjectMeetingSummaryVo(
    Long id,
    String title,
    String content,
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
    public static ProjectMeetingSummaryVo of(
            Long id,
            String title,
            String content,
            Integer participantNumber,
            String creatorName,
            boolean isEditable,
            OffsetDateTime createdAt,
            String meetingAttachedUrl,
            String meetingAttachedTitle) {
        return new ProjectMeetingSummaryVo(id, title,
                content, participantNumber, creatorName, isEditable,
                createdAt, meetingAttachedUrl, meetingAttachedTitle);
    }
} 