package org.certis.studyplatform.study.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * StudyMeetingLink JPA Entity
 *
 * Clean Architecture Infrastructure Layer
 * 프로젝트 미팅 링크 데이터베이스 테이블과 매핑되는 엔티티
 */
@Entity
@Table(name = "study_meeting_link")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE study_meeting_link SET deleted_at = NOW() WHERE id = ?")
public class StudyMeetingLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "study_id")
    private Long studyId;

    @Column(nullable = false, name = "member_id")
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, name = "attached_url", length = 500)
    private String attachedUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}