package org.certis.studyplatform.study.domain;

import lombok.Getter;

@Getter
public enum StudyStatus {
    READY("준비 중"),
    APPROVED("승인됨"),
    INPROGRESS("진행 중"),
    COMPLETED("완료"),
    REJECTED("중단됨");

    private final String description;

    StudyStatus(String description) {
        this.description = description;
    }

    /**
     * 설명으로부터 StudyStatus 찾기
     */
    public static StudyStatus fromDescription(String description) {
        if (description == null) {
            return READY;
        }

        for (StudyStatus status : values()) {
            if (status.description.equals(description)) {
                return status;
            }
        }
        return READY;
    }

    /**
     * 상태 문자열로부터 StudyStatus 찾기
     */
    public static StudyStatus fromStatusString(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            return READY;
        }

        String trimmed = statusString.trim().toUpperCase();

        try {
            return StudyStatus.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // 기존 값들과의 호환성
            return switch (trimmed) {
                case "IN_PROGRESS" -> INPROGRESS;
                case "PLANNING" -> READY;
                case "ON_HOLD" -> REJECTED;
                case "CANCELLED" -> REJECTED;
                case "RECRUITING" -> READY;
                case "NONE" -> READY;
                case "APPROVED" -> APPROVED;
                default -> READY;
            };
        }
    }

    public boolean isReady() {
        return this == READY;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isInProgress() {
        return this == INPROGRESS;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean isActive() {
        return this == READY || this == APPROVED || this == INPROGRESS;
    }
}
