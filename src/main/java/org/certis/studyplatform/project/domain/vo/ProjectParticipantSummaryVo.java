package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Project Participant Summary VO
 *
 * 프로젝트 참가자 요약 정보 VO (목록 조회용)
 */
public record ProjectParticipantSummaryVo(
        Long id,
        Long memberId,
        String memberName,
        ProjectParticipantStatus status,
        OffsetDateTime createdAt
) {}