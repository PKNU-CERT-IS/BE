package org.certis.studyplatform.shared.domain;

import lombok.Getter;

/**
 * Result submit status for Project/Study end report lifecycle.
 */
@Getter
public enum ResultSubmitStatus {
    READY("준비 중"),
    INPROGRESS("진행 중"),
    COMPLETED("완료"),
    REJECTED("중단됨");

    private final String description;

    ResultSubmitStatus(String description) {
        this.description = description;
    }

    /**
     * 설명으로부터 ResultSubmitStatus 찾기
     */
    public static ResultSubmitStatus fromDescription(String description) {
        if (description == null) {
            return READY;
        }

        for (ResultSubmitStatus status : values()) {
            if (status.description.equals(description)) {
                return status;
            }
        }
        return READY;
    }


    public boolean isReady() {
        return this == READY;
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
        return this == READY || this == INPROGRESS;
    }
}


