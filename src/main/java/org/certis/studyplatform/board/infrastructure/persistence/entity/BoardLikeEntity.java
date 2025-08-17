package org.certis.studyplatform.board.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Board Like Entity
 * 
 * 게시판 좋아요를 저장하는 JPA Entity
 */
@Entity
@Table(name = "board_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class BoardLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "like_number", nullable = false)
    private Integer likeNumber;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(toBuilder = true)
    private BoardLikeEntity(Long id, Long boardId, Long memberId, Integer likeNumber, OffsetDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.memberId = memberId;
        this.likeNumber = likeNumber;
        this.updatedAt = updatedAt;
    }
} 