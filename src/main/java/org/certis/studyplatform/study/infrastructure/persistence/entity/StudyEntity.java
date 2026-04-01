package org.certis.studyplatform.study.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.domain.StudyStatus;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;

/**
 * study JPA Entity
 *
 * Clean Architecture Infrastructure Layer
 * 데이터베이스 테이블과 매핑되는 엔티티
 */
@Entity
@Table(name = "study")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE study SET deleted_at = NOW() WHERE id = ?")
public class StudyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String subcategory;

    @Column(nullable = false, name = "max_participants_number")
    private Integer maxParticipantsNumber;

    @Column(nullable = false, name = "approved_slots_used")
    @Builder.Default
    private Integer approvedSlotsUsed = 0;

    @Column(nullable = false, name = "started_at")
    private OffsetDateTime startedAt;

    @Column(nullable = false, name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    @Builder.Default
    private StudyStatus status = StudyStatus.READY;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ===== Result submission fields =====
    @Column(name = "result_submitted_at")
    private OffsetDateTime resultSubmittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_submit_status", nullable = false)
    @Builder.Default
    private ResultSubmitStatus resultSubmitStatus = ResultSubmitStatus.READY;

    // Single URL for result attachment
    @Column(name = "result_attached_url")
    private String resultAttachmentUrl;
}
