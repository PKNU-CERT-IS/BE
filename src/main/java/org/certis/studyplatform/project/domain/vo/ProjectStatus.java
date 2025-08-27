package org.certis.studyplatform.project.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Project Status Enum
 *
 * 프로젝트의 현재 상태를 나타내는 열거형
 * activeContext.md에 정의된 상태 체계를 따름
 */
@Getter
@RequiredArgsConstructor
public enum ProjectStatus {
    READY("준비중", "프로젝트 시작 전"),
    INPROGRESS("진행중", "프로젝트 진행 중"),
    COMPLETED("완료", "프로젝트 완료됨"),
    REJECTED("거절됨", "프로젝트 승인 거절됨");

    private final String displayName;
    private final String description;

    /**
     * 문자열로부터 ProjectStatus를 찾는 메서드
     */
    public static ProjectStatus fromString(String status) {
        for (ProjectStatus projectStatus : ProjectStatus.values()) {
            if (projectStatus.name().equalsIgnoreCase(status)) {
                return projectStatus;
            }
        }
        throw new IllegalArgumentException("Unknown project status: " + status);
    }
}