package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Meeting Value Object
 *
 * 프로젝트 회의록 상세 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record ProjectMeetingVo(
    Long id,
    Long projectId,
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
    public static ProjectMeetingVo of(
            Long id,
            Long projectId,
            String title,
            String content,
            List<Long> participantIds,
            Long writerId,
            boolean isEditable,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new ProjectMeetingVo(
            id, projectId, title, content,
            participantIds, writerId, isEditable,
            createdAt, updatedAt
        );
    }
} 