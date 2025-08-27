package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

public record ProjectParticipantCreatedVo(
        Long id,
        Long projectId,
        Long memberId,
        ProjectParticipantStatus status,
        OffsetDateTime createdAt
) {

    public static ProjectParticipantCreatedVo of(Long id,
                                               Long projectId,
                                               Long memberId,
                                               ProjectParticipantStatus status,
                                                 OffsetDateTime createdAt) {
        return new ProjectParticipantCreatedVo(
                id,
                projectId,
                memberId,
                status,
                createdAt
        );
    }
}
