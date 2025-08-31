package org.certis.studyplatform.project.domain;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.project.domain.vo.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Domain Entity
 *
 * @deprecated 이 Domain Entity는 향후 사용하지 않을 예정입니다.
 * Infrastructure Layer의 ProjectEntity와 Presentation Layer의 ResponseDTO를 직접 사용하는 방식으로 전환됩니다.
 * 현재는 호환성을 위해 유지되고 있으나, 새로운 개발에서는 사용을 지양해주세요.
 *
 * Clean Architecture Domain Layer
 * 비즈니스 로직과 규칙을 포함하는 핵심 도메인 객체
 */
@Deprecated(since = "2024-08", forRemoval = true)
@Getter
@Builder
public class Project {

    private final Long id;
    private final String title;
    private final String description;
    private final String category;
    private final String subCategory;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Long leaderId;
    private final String leaderName;
    private final List<String> techStack;
    private final String githubUrl;
    private final String planDocument;
    private final List<String> attachedFiles;
    private final Integer maxParticipants;
    private final Integer currentParticipants;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    // Additional fields for compatibility with VOs
    private final List<String> externalUrls;
    private final List<String> tags;
    private final String content;
    private final String demoUrl;

    /**
     * 프로젝트 상태를 동적으로 계산
     * 시작일과 종료일을 기준으로 현재 상태를 결정
     */
    public ProjectStatus getStatus() {
        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(startDate)) {
            return ProjectStatus.READY;
        } else if (now.isAfter(endDate)) {
            return ProjectStatus.COMPLETED;
        } else {
            return ProjectStatus.INPROGRESS;
        }
    }

    /**
     * 프로젝트 참여 가능 여부 확인
     */
    public boolean canJoin() {
        return currentParticipants < maxParticipants && getStatus() == ProjectStatus.READY;
    }

    /**
     * 프로젝트 수정 가능 여부 확인
     */
    public boolean canModify(Long requesterId) {
        return leaderId.equals(requesterId) && getStatus() != ProjectStatus.COMPLETED;
    }

    /**
     * 프로젝트 진행률 계산
     */
    public double getProgress() {
        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(startDate)) {
            return 0.0;
        } else if (now.isAfter(endDate)) {
            return 100.0;
        } else {
            long totalDays = java.time.Duration.between(startDate, endDate).toDays();
            long elapsedDays = java.time.Duration.between(startDate, now).toDays();
            return Math.min(100.0, (double) elapsedDays / totalDays * 100);
        }
    }

    /**
     * D-Day 계산
     */
    public long getDDay() {
        OffsetDateTime now = OffsetDateTime.now();

        if (getStatus() == ProjectStatus.READY) {
            return java.time.Duration.between(now, startDate).toDays();
        } else if (getStatus() == ProjectStatus.INPROGRESS) {
            return java.time.Duration.between(now, endDate).toDays();
        } else {
            return 0;
        }
    }


}