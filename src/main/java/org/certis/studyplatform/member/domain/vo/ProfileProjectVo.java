package org.certis.studyplatform.member.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

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
        String status, // PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD, CANCELLED
        String role, // PROJECT_LEADER, TECH_LEADER, MEMBER
        OffsetDateTime joinedAt,
        OffsetDateTime projectStartDate,
        OffsetDateTime projectEndDate,
        String repositoryUrl,
        String deployUrl,
        Integer memberCount,
        List<String> techStack
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileProjectVo {
        // List의 불변성 보장
        techStack = techStack != null ? List.copyOf(techStack) : List.of();
    }

    /**
     * 편의 생성자 - 기술 스택 없이
     */
    public ProfileProjectVo(Long projectId, String title, String description, String status,
                            String role, OffsetDateTime joinedAt, OffsetDateTime projectStartDate,
                            OffsetDateTime projectEndDate, String repositoryUrl, String deployUrl,
                            Integer memberCount) {
        this(projectId, title, description, status, role, joinedAt, projectStartDate,
                projectEndDate, repositoryUrl, deployUrl, memberCount, List.of());
    }

    /**
     * 프로젝트가 진행 중인지 확인
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * 프로젝트가 완료되었는지 확인
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 계획 단계인지 확인
     */
    public boolean isPlanning() {
        return "PLANNING".equals(status);
    }

    /**
     * 보류 상태인지 확인
     */
    public boolean isOnHold() {
        return "ON_HOLD".equals(status);
    }

    /**
     * 프로젝트 리더인지 확인
     */
    public boolean isProjectLeader() {
        return "PROJECT_LEADER".equals(role);
    }

    /**
     * 기술 리더인지 확인
     */
    public boolean isTechLeader() {
        return "TECH_LEADER".equals(role);
    }

    /**
     * 리더 역할인지 확인 (프로젝트 리더 또는 기술 리더)
     */
    public boolean isLeader() {
        return isProjectLeader() || isTechLeader();
    }

    /**
     * 멤버인지 확인
     */
    public boolean isMember() {
        return "MEMBER".equals(role);
    }

    /**
     * 기술 스택 배열 반환 (Presentation Layer 호환)
     */
    public String[] getTechStackArray() {
        return techStack.toArray(new String[0]);
    }

    /**
     * 배포 가능한 프로젝트인지 확인
     */
    public boolean isDeployable() {
        return deployUrl != null && !deployUrl.trim().isEmpty();
    }

    /**
     * 저장소가 있는지 확인
     */
    public boolean hasRepository() {
        return repositoryUrl != null && !repositoryUrl.trim().isEmpty();
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
        return techStack.stream()
                .anyMatch(tech -> tech.equalsIgnoreCase(technology));
    }
}