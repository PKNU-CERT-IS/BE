package org.certis.studyplatform.board.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Board View Entity
 * 
 * 게시판 조회수를 저장하는 JPA Entity
 */
@Entity
@Table(name = "board_view")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class BoardViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "view_number", nullable = false)
    private Integer viewNumber;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(toBuilder = true)
    private BoardViewEntity(Long id, Long boardId, Integer viewNumber, OffsetDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.viewNumber = viewNumber;
        this.updatedAt = updatedAt;
    }
} 