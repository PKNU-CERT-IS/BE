package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;

/**
 * Study Meeting Updated Value Object
 *
 * 스터디 회의록 수정 결과를 나타내는 불변 객체
 */
public record StudyMeetingUpdatedVo(
    Long id,
    String title,
    String content,
    Integer participantNumber,
    OffsetDateTime updatedAt
) {
    public static StudyMeetingUpdatedVo of(
        Long id,
        String title,
        String content,
        Integer participantNumber,
        OffsetDateTime updatedAt
    ) {
        return new StudyMeetingUpdatedVo(
            id, title, content, participantNumber, updatedAt
        );
    }
} 