package org.certis.studyplatform.project.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Admin Project Participant Approval Response DTO
 *
 * 관리자가 프로젝트 참가 신청을 승인/거절한 결과를 반환하는 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdminProjectParticipantApprovalResponseDto {

    private Long participantId;
    private Long projectId;
    private String projectTitle;
    private Long memberId;
    private String memberName;
    private ProjectParticipantStatus status;
    private String reason;
    private Long adminId;
    private String adminName;
    private OffsetDateTime processedAt;

    // toString for logging
    @Override
    public String toString() {
        return "AdminProjectParticipantApprovalResponseDto{" +
                "participantId=" + participantId +
                ", projectId=" + projectId +
                ", projectTitle='" + projectTitle + '\'' +
                ", memberId=" + memberId +
                ", memberName='" + memberName + '\'' +
                ", status=" + status +
                ", adminId=" + adminId +
                ", processedAt=" + processedAt +
                '}';
    }
}
