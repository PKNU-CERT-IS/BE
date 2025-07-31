package org.certis.studyplatform.study.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.certis.studyplatform.study.domain.model.Study;
import org.certis.studyplatform.study.domain.vo.*;

import java.time.ZonedDateTime;
import java.util.Arrays;

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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "text[]", nullable = false)
    private String[] skills;

    @Column(nullable = false)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "started_at", nullable = false)
    private ZonedDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private ZonedDateTime endedAt;

    @Column(name = "max_participants_number", nullable = false)
    private Integer maxParticipantsNumber;

    @Column(nullable = false)
    private String description;

    @Builder
    private StudyEntity(Long id, Long memberId, String title, String content, String type,
                        String[] skills, String category, ZonedDateTime createdAt, ZonedDateTime updatedAt,
                        ZonedDateTime deletedAt, ZonedDateTime startedAt, ZonedDateTime endedAt,
                        Integer maxParticipantsNumber, String description) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.skills = skills;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.maxParticipantsNumber = maxParticipantsNumber;
        this.description = description;
    }

    public static StudyEntity fromDomain(Study study) {
        return StudyEntity.builder()
                .id(study.getId() != null ? study.getId().value() : null)
                .memberId(study.getMemberId().value())
                .title(study.getTitle())
                .content(study.getContent())
                .type(study.getType())
                .skills(study.getSkills().toArray())
                .category(study.getCategory())
                .createdAt(study.getCreatedAt())
                .updatedAt(study.getUpdatedAt())
                .startedAt(study.getStartedAt())
                .endedAt(study.getEndedAt())
                .maxParticipantsNumber(study.getMaxParticipantsNumber())
                .description(study.getDescription())
                .build();
    }

    public Study toDomain() {
        return new Study(
                this.id != null ? new StudyId(this.id) : null,
                new MemberId(this.memberId),
                this.title,
                this.content,
                this.type,
                Skills.of(Arrays.asList(this.skills)),
                this.category,
                this.createdAt,
                this.updatedAt,
                this.startedAt,
                this.endedAt,
                this.maxParticipantsNumber,
                this.description
        );
    }
}