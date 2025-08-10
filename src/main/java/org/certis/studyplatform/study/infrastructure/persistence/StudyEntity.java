package org.certis.studyplatform.study.infrastructure.persistence;

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
 * Study Entity
 * 
 * 스터디를 저장하는 JPA Entity
 */
@Entity
@Table(name = "study")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE study SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class StudyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "skills", nullable = false, columnDefinition = "text[]")
    private Object skills;

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
    private StudyEntity(Long id, Long memberId, String title, String content,
                       String type, Object skills, String category, String description,
                       OffsetDateTime startedAt, OffsetDateTime endedAt, Integer maxParticipantsNumber,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.skills = skills;
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
     * 현재 시각을 기준으로 스터디 상태를 동적으로 계산
     * 
     * 로직:
     * 1. deletedAt이 null이 아니면 REJECTED
     * 2. 현재 시각이 startedAt보다 이전이면 READY
     * 3. 현재 시각이 startedAt과 endedAt 사이면 INPROGRESS
     * 4. 현재 시각이 endedAt보다 이후이고 deletedAt이 null이면 COMPLETED
     */
    public StudyStatus getCurrentStatus() {
        return calculateStudyStatus(OffsetDateTime.now());
    }

    /**
     * 특정 시각을 기준으로 스터디 상태를 계산 (테스트용)
     */
    public StudyStatus calculateStudyStatus(OffsetDateTime currentTime) {
        // 삭제된 스터디는 REJECTED
        if (deletedAt != null) {
            return StudyStatus.REJECTED;
        }

        // 시작 전이면 READY
        if (currentTime.isBefore(startedAt)) {
            return StudyStatus.READY;
        }

        // 종료 후면 COMPLETED
        if (currentTime.isAfter(endedAt)) {
            return StudyStatus.COMPLETED;
        }

        // 진행 중이면 INPROGRESS
        return StudyStatus.INPROGRESS;
    }

    /**
     * 스터디가 활성 상태인지 확인 (READY 또는 INPROGRESS)
     */
    public boolean isActive() {
        StudyStatus status = getCurrentStatus();
        return status.isActive();
    }

    /**
     * 스터디가 시작되었는지 확인
     */
    public boolean isStarted() {
        return OffsetDateTime.now().isAfter(startedAt) || OffsetDateTime.now().isEqual(startedAt);
    }

    /**
     * 스터디가 종료되었는지 확인
     */
    public boolean isEnded() {
        return OffsetDateTime.now().isAfter(endedAt);
    }

    /**
     * 스터디가 삭제(중단)되었는지 확인
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 스터디 기간 내에 있는지 확인
     */
    public boolean isWithinStudyPeriod() {
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(startedAt) && !now.isAfter(endedAt) && !isDeleted();
    }
} 