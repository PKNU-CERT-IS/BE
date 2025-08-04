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

import java.time.ZonedDateTime;

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
    private ZonedDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private ZonedDateTime endedAt;

    @Column(name = "max_participants_number", nullable = false)
    private Integer maxParticipantsNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Builder(toBuilder = true)
    private StudyEntity(Long id, Long memberId, String title, String content,
                       String type, Object skills, String category, String description,
                       ZonedDateTime startedAt, ZonedDateTime endedAt, Integer maxParticipantsNumber,
                       ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
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
} 