package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Meeting Created Value Object
 *
 * 스터디 회의록 생성 결과를 나타내는 불변 객체
 */
public record StudyMeetingCreatedVo(
    Long id,
    Long studyId,
    String title,
    String content,
    List<Long> participantIds,
    Long writerId,
    OffsetDateTime createdAt
) {
    public static StudyMeetingCreatedVo of(
        Long id,
        Long studyId, String title,
        String content,
        List<Long> participantIds,
        Long writerId,
        OffsetDateTime createdAt
    ) {
        return new StudyMeetingCreatedVo(
            id, studyId, title, content, participantIds,
            writerId, createdAt
        );
    }
} 