package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Meeting Value Object
 *
 * 스터디 회의록 상세 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record StudyMeetingVo(
    Long id,
    Long studyId,
    String title,
    String content,
    List<Long> participantIds,
    Long writerId,
    boolean isEditable,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    /**
     * 기본 팩토리 메서드
     */
    public static StudyMeetingVo of(
            Long id,
            Long studyId,
            String title,
            String content,
            List<Long> participantIds,
            Long writerId,
            boolean isEditable,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new StudyMeetingVo(
            id, studyId, title, content,
            participantIds, writerId, isEditable,
            createdAt, updatedAt
        );
    }
} 