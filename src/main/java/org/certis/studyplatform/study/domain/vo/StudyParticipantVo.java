package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

public record StudyParticipantVo(
        Long id,
        Long studyId,
        Long memberId,
        String memberName,
        StudyParticipantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 새로운 스터디 참가 신청 생성
     */
    public static StudyParticipantVo createNew(Long studyId, Long memberId) {
        return new StudyParticipantVo(
                null,
                studyId,
                memberId,
                null, // memberName은 저장 후 조회 시 설정
                StudyParticipantStatus.PENDING,
                null, // createdAt은 저장 시 자동 설정
                null  // updatedAt은 저장 시 자동 설정
        );
    }

    /**
     * 승인된 상태의 새로운 참가자 생성 (스터디 생성자용)
     *
     * @param studyId 스터디 ID
     * @param memberId 멤버 ID
     * @return 승인된 상태의 StudyParticipantVo
     */
    public static StudyParticipantVo createApproved(Long studyId, Long memberId) {

        return new StudyParticipantVo(
                null,
                studyId,
                memberId,
                null,
                StudyParticipantStatus.APPROVED,
                null,
                OffsetDateTime.now()
        );
    }

    /**
     * 상태 변경 (승인/거절/취소)
     */
    public StudyParticipantVo updateStatus(StudyParticipantStatus newStatus) {
        return new StudyParticipantVo(
                this.id,
                this.studyId,
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
        return StudyParticipantStatus.PENDING.equals(this.status);
    }

    /**
     * 승인된 상태인지 확인
     */
    public boolean isApproved() {
        return StudyParticipantStatus.APPROVED.equals(this.status);
    }

    /**
     * 거절된 상태인지 확인
     */
    public boolean isRejected() {
        return StudyParticipantStatus.REJECTED.equals(this.status);
    }

    /**
     * 취소된 상태인지 확인
     */
    public boolean isCancelled() {
        return StudyParticipantStatus.CANCELLED.equals(this.status);
    }
}
