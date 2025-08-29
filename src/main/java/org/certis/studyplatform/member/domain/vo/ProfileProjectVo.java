package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.project.domain.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * 프로젝트 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record ProfileProjectVo(
        Long projectId,
        String title,
        String description,
        ProjectStatus projectStatus,
        OffsetDateTime projectStartDate,
        OffsetDateTime projectEndDate,
        String[] tags
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileProjectVo {
        // 배열의 불변성 보장
        tags = tags != null ? tags.clone() : new String[0];
    }

    /**
     * 프로젝트가 진행 중인지 확인
     */
    public boolean isInProgress() {
        return ProjectStatus.INPROGRESS.equals(projectStatus);
    }

    /**
     * 프로젝트가 완료되었는지 확인
     */
    public boolean isCompleted() {
        return ProjectStatus.COMPLETED.equals(projectStatus);
    }

    /**
     * 준비 단계인지 확인
     */
    public boolean isReady() {
        return ProjectStatus.READY.equals(projectStatus);
    }

    /**
     * 중단되었는지 확인
     */
    public boolean isRejected() {
        return ProjectStatus.REJECTED.equals(projectStatus);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return projectStatus != null && projectStatus.isActive();
    }

    /**
     * 기술 스택 배열 반환 (Presentation Layer 호환)
     */
    public String[] getTagsArray() {
        return tags.clone();
    }

    /**
     * 프로젝트 기간 내에 있는지 확인
     */
    public boolean isWithinProjectPeriod() {
        if (projectStartDate == null || projectEndDate == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(projectStartDate) && !now.isAfter(projectEndDate);
    }

    /**
     * 특정 기술을 사용하는지 확인
     */
    public boolean usesTechnology(String technology) {
        if (technology == null || tags == null) {
            return false;
        }
        return Arrays.stream(tags)
                .anyMatch(tech -> tech.equalsIgnoreCase(technology));
    }
}