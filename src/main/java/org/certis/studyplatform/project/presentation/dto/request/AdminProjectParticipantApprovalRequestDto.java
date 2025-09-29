package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin Project Participant Approval Request DTO
 *
 * 관리자가 프로젝트 참가 신청을 승인/거절할 때 사용하는 요청 DTO
 */
@Getter
@Setter
public class AdminProjectParticipantApprovalRequestDto {

    @NotNull(message = "참가자 ID는 필수입니다")
    @Positive(message = "참가자 ID는 양수여야 합니다")
    private Long participantId;

    private String reason; // 승인/거절 사유 (선택사항)

    // toString for logging
    @Override
    public String toString() {
        return "AdminProjectParticipantApprovalRequestDto{" +
                "participantId=" + participantId +
                ", reason='" + reason + '\'' +
                '}';
    }
}
