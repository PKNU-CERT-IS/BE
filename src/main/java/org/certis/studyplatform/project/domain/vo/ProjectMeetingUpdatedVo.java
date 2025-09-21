package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Meeting Updated Value Object
 *
 * 프로젝트 회의록 수정 결과를 나타내는 불변 객체
 */
public record ProjectMeetingUpdatedVo(
    Long id,
    String title,
    String content,
    Integer participantNumber,
    OffsetDateTime updatedAt
) {
    public static ProjectMeetingUpdatedVo of(
        Long id,
        String title,
        String content,
        Integer participantNumber,
        OffsetDateTime updatedAt
    ) {
        return new ProjectMeetingUpdatedVo(
            id, title, content, participantNumber, updatedAt
        );
    }
} 