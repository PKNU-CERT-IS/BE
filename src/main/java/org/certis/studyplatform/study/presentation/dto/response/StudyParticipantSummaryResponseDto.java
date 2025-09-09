package org.certis.studyplatform.study.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Study Participant Summary Response DTO
 *
 * 프로젝트 참가자 요약 정보 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
public class StudyParticipantSummaryResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;
    private MemberGrade memberGrade;
    private StudyParticipantStatus status;
    private OffsetDateTime createdAt;
}
