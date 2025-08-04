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

import java.time.ZonedDateTime;

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
    private ProjectEntity(Long id, Long memberId, String title, String content,
                         Object skills, String difficulty, String category, String description,
                         ZonedDateTime startedAt, ZonedDateTime endedAt, Integer maxParticipantsNumber,
                         ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
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
} 