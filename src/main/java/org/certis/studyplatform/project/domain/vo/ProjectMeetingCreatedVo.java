package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;

/**
 * Project Meeting Created Value Object
 *
 * 프로젝트 회의록 생성 결과를 나타내는 불변 객체
 */
public record ProjectMeetingCreatedVo(
    Long id,
    Long projectId,
    String title,
    String content,
    Integer participantNumber,
    Long writerId,
    OffsetDateTime createdAt
) {
    public static ProjectMeetingCreatedVo of(
        Long id,
        Long projectId,
        String title,
        String content,
        Integer participantNumber,
        Long writerId,
        OffsetDateTime createdAt
    ) {
        return new ProjectMeetingCreatedVo(
            id, projectId, title, content, participantNumber,
            writerId, createdAt
        );
    }
} 