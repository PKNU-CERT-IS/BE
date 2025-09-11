package org.certis.studyplatform.blog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Blog View Entity
 *
 * 블로그 조회수를 저장하는 JPA Entity
 */
@Entity
@Table(name = "blog_view")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class BlogViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id")
    private Long blogId;

    @Column(name = "view_number")
    private Integer viewNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder(toBuilder = true)
    private BlogViewEntity(Long id, Long blogId, Integer viewNumber, OffsetDateTime createdAt) {
        this.id = id;
        this.blogId = blogId;
        this.viewNumber = viewNumber;
        this.createdAt = createdAt;
    }
}