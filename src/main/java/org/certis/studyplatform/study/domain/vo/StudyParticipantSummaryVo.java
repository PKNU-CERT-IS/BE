package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Study Participant Summary VO
 *
 * 스터디 참가자 요약 정보 VO (목록 조회용)
 */
public record StudyParticipantSummaryVo(
        Long id,
        Long studyId,
        Long memberId,
        String memberName,
        MemberGrade memberGrade,
        String memberProfileImageUrl,
        String studyTitle,
        StudyParticipantStatus status,
        OffsetDateTime createdAt
) {}