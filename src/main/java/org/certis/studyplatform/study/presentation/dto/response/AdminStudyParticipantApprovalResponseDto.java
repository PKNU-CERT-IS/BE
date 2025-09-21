package org.certis.studyplatform.study.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Admin Study Participant Approval Response DTO
 *
 * 관리자가 스터디 참가 신청을 승인/거절한 결과를 반환하는 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudyParticipantApprovalResponseDto {

    private Long participantId;
    private Long studyId;
    private String studyTitle;
    private Long memberId;
    private String memberName;
    private StudyParticipantStatus status;
    private String reason;
    private Long adminId;
    private String adminName;
    private OffsetDateTime processedAt;

    // toString for logging
    @Override
    public String toString() {
        return "AdminStudyParticipantApprovalResponseDto{" +
                "participantId=" + participantId +
                ", studyId=" + studyId +
                ", studyTitle='" + studyTitle + '\'' +
                ", memberId=" + memberId +
                ", memberName='" + memberName + '\'' +
                ", status=" + status +
                ", adminId=" + adminId +
                ", processedAt=" + processedAt +
                '}';
    }
}
