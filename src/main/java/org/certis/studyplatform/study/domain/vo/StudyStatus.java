package org.certis.studyplatform.study.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Study Status Enum
 *
 * 스터디의 현재 상태를 나타내는 열거형
 * activeContext.md에 정의된 상태 체계를 따름
 */
@Getter
@RequiredArgsConstructor
public enum StudyStatus {
    READY("준비중", "스터디 시작 전"),
    INPROGRESS("진행중", "스터디 진행 중"),
    COMPLETED("완료", "스터디 완료됨"),
    REJECTED("거절됨", "스터디 승인 거절됨");

    private final String displayName;
    private final String description;

    /**
     * 문자열로부터 StudyStatus를 찾는 메서드
     */
    public static StudyStatus fromString(String status) {
        for (StudyStatus studyStatus : StudyStatus.values()) {
            if (studyStatus.name().equalsIgnoreCase(status)) {
                return studyStatus;
            }
        }
        throw new IllegalArgumentException("Unknown study status: " + status);
    }
}