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

//import org.certis.studyplatform.project.domain.model.ProjectParticipant;
//import org.certis.studyplatform.project.domain.vo.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "project_participant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE project_participant SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ProjectParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Builder
    private ProjectParticipantEntity(Long id, Long projectId, Long memberId, ZonedDateTime createdAt,
                                     ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
        this.id = id;
        this.projectId = projectId;
        this.memberId = memberId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

//    public static ProjectParticipantEntity fromDomain(ProjectParticipant participant) {
//        return ProjectParticipantEntity.builder()
//                .id(participant.getId() != null ? participant.getId().value() : null)
//                .projectId(participant.getProjectId().value())
//                .memberId(participant.getMemberId().value())
//                .createdAt(participant.getCreatedAt())
//                .updatedAt(participant.getUpdatedAt())
//                .build();
//    }
//
//    public ProjectParticipant toDomain() {
//        return new ProjectParticipant(
//                this.id != null ? new ProjectParticipantId(this.id) : null,
//                new ProjectId(this.projectId),
//                new MemberId(this.memberId),
//                this.createdAt,
//                this.updatedAt
//        );
//    }
}