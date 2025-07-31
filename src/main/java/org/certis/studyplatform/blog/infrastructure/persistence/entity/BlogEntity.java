package org.certis.studyplatform.blog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.certis.studyplatform.blog.domain.model.Blog;
import org.certis.studyplatform.blog.domain.vo.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "blog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE blog SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class BlogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "study_id")
    private Long studyId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

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

    @Column(nullable = false)
    private String description;

    @Builder
    private BlogEntity(Long id, Long memberId, Long studyId, Long projectId, String title,
                       String content, String category, ZonedDateTime createdAt, ZonedDateTime updatedAt,
                       ZonedDateTime deletedAt, String description) {
        this.id = id;
        this.memberId = memberId;
        this.studyId = studyId;
        this.projectId = projectId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.description = description;
    }

    public static BlogEntity fromDomain(Blog blog) {
        return BlogEntity.builder()
                .id(blog.getId() != null ? blog.getId().value() : null)
                .memberId(blog.getMemberId().value())
                .studyId(blog.getStudyId() != null ? blog.getStudyId().value() : null)
                .projectId(blog.getProjectId() != null ? blog.getProjectId().value() : null)
                .title(blog.getTitle())
                .content(blog.getContent())
                .category(blog.getCategory())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .description(blog.getDescription())
                .build();
    }

    public Blog toDomain() {
        return new Blog(
                this.id != null ? new BlogId(this.id) : null,
                new MemberId(this.memberId),
                this.studyId != null ? new StudyId(this.studyId) : null,
                this.projectId != null ? new ProjectId(this.projectId) : null,
                this.title,
                this.content,
                this.category,
                this.createdAt,
                this.updatedAt,
                this.description
        );
    }
}