package org.certis.studyplatform.study.domain;

public enum StudyPariticipantStatus {

    /**
     * 참가 신청 상태 (대기 중)
     */
    PENDING,

    /**
     * 참가 승인됨
     */
    APPROVED,

    /**
     * 참가 거절됨
     */
    REJECTED,

    /**
     * 참가 신청 취소됨
     */
    CANCELLED
}
