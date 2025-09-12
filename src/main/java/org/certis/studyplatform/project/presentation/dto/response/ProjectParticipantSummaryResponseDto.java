package org.certis.studyplatform.project.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Project Participant Summary Response DTO
 *
 * 프로젝트 참가자 요약 정보 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
public class ProjectParticipantSummaryResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;
    private MemberGrade memberGrade;
    private ProjectParticipantStatus status;
    private OffsetDateTime createdAt;
}
