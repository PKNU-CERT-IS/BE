package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Meeting Updated Value Object
 *
 * 스터디 회의록 수정 결과를 나타내는 불변 객체
 */
public record StudyMeetingUpdatedVo(
    Long id,
    String title,
    String content,
    List<Long> participantIds,
    OffsetDateTime updatedAt
) {
    public static StudyMeetingUpdatedVo of(
        Long id,
        String title,
        String content,
        List<Long> participantIds,
        OffsetDateTime updatedAt
    ) {
        return new StudyMeetingUpdatedVo(
            id, title, content, participantIds, updatedAt
        );
    }
} 