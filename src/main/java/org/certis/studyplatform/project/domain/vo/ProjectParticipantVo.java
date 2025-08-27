package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

public record ProjectParticipantVo(
        Long id,
        Long projectId,
        Long memberId,
        String memberName,
        ProjectParticipantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 새로운 프로젝트 참가 신청 생성
     */
    public static ProjectParticipantVo createNew(Long projectId, Long memberId) {
        return new ProjectParticipantVo(
                null,
                projectId,
                memberId,
                null, // memberName은 저장 후 조회 시 설정
                ProjectParticipantStatus.PENDING,
                null, // createdAt은 저장 시 자동 설정
                null  // updatedAt은 저장 시 자동 설정
        );
    }

    /**
     * 승인된 상태의 새로운 참가자 생성 (프로젝트 생성자용)
     *
     * @param projectId 프로젝트 ID
     * @param memberId 멤버 ID
     * @return 승인된 상태의 ProjectParticipantVo
     */
    public static ProjectParticipantVo createApproved(Long projectId, Long memberId) {

        return new ProjectParticipantVo(
                null,
                projectId,
                memberId,
                null,
                ProjectParticipantStatus.APPROVED,
                null,
                OffsetDateTime.now()
        );
    }

    /**
     * 상태 변경 (승인/거절/취소)
     */
    public ProjectParticipantVo updateStatus(ProjectParticipantStatus newStatus) {
        return new ProjectParticipantVo(
                this.id,
                this.projectId,
                this.memberId,
                this.memberName,
                newStatus,
                this.createdAt,
                this.updatedAt // updatedAt은 저장 시 자동 갱신
        );
    }

    /**
     * 참가 신청 상태인지 확인
     */
    public boolean isPending() {
        return ProjectParticipantStatus.PENDING.equals(this.status);
    }

    /**
     * 승인된 상태인지 확인
     */
    public boolean isApproved() {
        return ProjectParticipantStatus.APPROVED.equals(this.status);
    }

    /**
     * 거절된 상태인지 확인
     */
    public boolean isRejected() {
        return ProjectParticipantStatus.REJECTED.equals(this.status);
    }

    /**
     * 취소된 상태인지 확인
     */
    public boolean isCancelled() {
        return ProjectParticipantStatus.CANCELLED.equals(this.status);
    }
}
