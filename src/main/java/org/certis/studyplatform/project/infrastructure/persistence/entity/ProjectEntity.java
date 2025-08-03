package org.certis.studyplatform.project.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

//import org.certis.studyplatform.project.domain.model.Project;
//import org.certis.studyplatform.project.domain.vo.*;

import java.time.ZonedDateTime;
import java.util.Arrays;

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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(columnDefinition = "text[]", nullable = false)
    private String[] skills;

    @Column(nullable = false)
    private String difficulty;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(name = "started_at", nullable = false)
    private ZonedDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private ZonedDateTime endedAt;

    @Column(name = "max_participants_number", nullable = false)
    private Integer maxParticipantsNumber;

    @Builder
    private ProjectEntity(Long id, Long memberId, String title, String content, String[] skills,
                          String difficulty, ZonedDateTime createdAt, ZonedDateTime updatedAt,
                          ZonedDateTime deletedAt, String category, String description,
                          ZonedDateTime startedAt, ZonedDateTime endedAt, Integer maxParticipantsNumber) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.skills = skills;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.category = category;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.maxParticipantsNumber = maxParticipantsNumber;
    }

//    public static ProjectEntity fromDomain(Project project) {
//        return ProjectEntity.builder()
//                .id(project.getId() != null ? project.getId().value() : null)
//                .memberId(project.getMemberId().value())
//                .title(project.getTitle())
//                .content(project.getContent())
//                .skills(project.getSkills().toArray())
//                .difficulty(project.getDifficulty())
//                .createdAt(project.getCreatedAt())
//                .updatedAt(project.getUpdatedAt())
//                .category(project.getCategory())
//                .description(project.getDescription())
//                .startedAt(project.getStartedAt())
//                .endedAt(project.getEndedAt())
//                .maxParticipantsNumber(project.getMaxParticipantsNumber())
//                .build();
//    }
//
//    public Project toDomain() {
//        return new Project(
//                this.id != null ? new ProjectId(this.id) : null,
//                new MemberId(this.memberId),
//                this.title,
//                this.content,
//                Skills.of(Arrays.asList(this.skills)),
//                this.difficulty,
//                this.createdAt,
//                this.updatedAt,
//                this.category,
//                this.description,
//                this.startedAt,
//                this.endedAt,
//                this.maxParticipantsNumber
//        );
//    }
}