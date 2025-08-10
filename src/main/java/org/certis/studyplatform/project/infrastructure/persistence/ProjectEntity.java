package org.certis.studyplatform.project.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.OffsetDateTime;

/**
 * Project Entity
 * 
 * 프로젝트를 저장하는 JPA Entity
 */
@Entity
@Table(name = "project")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE project SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "skills", nullable = false, columnDefinition = "text[]")
    private Object skills;

    @Column(name = "difficulty", nullable = false)
    private String difficulty;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;

    @Column(name = "max_participants_number", nullable = false)
    private Integer maxParticipantsNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Builder(toBuilder = true)
    private ProjectEntity(Long id, Long memberId, String title, String content,
                         Object skills, String difficulty, String category, String description,
                         OffsetDateTime startedAt, OffsetDateTime endedAt, Integer maxParticipantsNumber,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.skills = skills;
        this.difficulty = difficulty;
        this.category = category;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.maxParticipantsNumber = maxParticipantsNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 현재 시각을 기준으로 프로젝트 상태를 동적으로 계산
     * 
     * 로직:
     * 1. deletedAt이 null이 아니면 REJECTED
     * 2. 현재 시각이 startedAt보다 이전이면 READY
     * 3. 현재 시각이 startedAt과 endedAt 사이면 INPROGRESS
     * 4. 현재 시각이 endedAt보다 이후이고 deletedAt이 null이면 COMPLETED
     */
    public ProjectStatus getCurrentStatus() {
        return calculateProjectStatus(OffsetDateTime.now());
    }

    /**
     * 특정 시각을 기준으로 프로젝트 상태를 계산 (테스트용)
     */
    public ProjectStatus calculateProjectStatus(OffsetDateTime currentTime) {
        // 삭제된 프로젝트는 REJECTED
        if (deletedAt != null) {
            return ProjectStatus.REJECTED;
        }

        // 시작 전이면 READY
        if (currentTime.isBefore(startedAt)) {
            return ProjectStatus.READY;
        }

        // 종료 후면 COMPLETED
        if (currentTime.isAfter(endedAt)) {
            return ProjectStatus.COMPLETED;
        }

        // 진행 중이면 INPROGRESS
        return ProjectStatus.INPROGRESS;
    }

    /**
     * 프로젝트가 활성 상태인지 확인 (READY 또는 INPROGRESS)
     */
    public boolean isActive() {
        ProjectStatus status = getCurrentStatus();
        return status.isActive();
    }

    /**
     * 프로젝트가 시작되었는지 확인
     */
    public boolean isStarted() {
        return OffsetDateTime.now().isAfter(startedAt) || OffsetDateTime.now().isEqual(startedAt);
    }

    /**
     * 프로젝트가 종료되었는지 확인
     */
    public boolean isEnded() {
        return OffsetDateTime.now().isAfter(endedAt);
    }

    /**
     * 프로젝트가 삭제(중단)되었는지 확인
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 프로젝트 기간 내에 있는지 확인
     */
    public boolean isWithinProjectPeriod() {
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(startedAt) && !now.isAfter(endedAt) && !isDeleted();
    }
} 