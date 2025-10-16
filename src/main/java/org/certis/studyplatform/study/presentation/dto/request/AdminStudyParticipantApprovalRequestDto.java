package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin Study Participant Approval Request DTO
 *
 * 관리자가 스터디 참가 신청을 승인/거절할 때 사용하는 요청 DTO
 * StudyJoinApproveRequestDto와 동일한 필드(studyId, memberId)를 사용합니다.
 */
@Getter
@Setter
public class AdminStudyParticipantApprovalRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    // toString for logging
    @Override
    public String toString() {
        return "AdminStudyParticipantApprovalRequestDto{" +
                "studyId=" + studyId +
                ", memberId=" + memberId +
                '}';
    }
}
