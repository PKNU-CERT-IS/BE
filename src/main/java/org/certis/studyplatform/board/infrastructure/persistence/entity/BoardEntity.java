package org.certis.studyplatform.board.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

//import org.certis.studyplatform.board.domain.model.Board;
//import org.certis.studyplatform.board.domain.vo.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "board")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE board SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class BoardEntity {

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
    private BoardEntity(Long id, Long memberId, String title, String content, String category,
                        ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt,
                        String description) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.description = description;
    }

//    public static BoardEntity fromDomain(Board board) {
//        return BoardEntity.builder()
//                .id(board.getId() != null ? board.getId().value() : null)
//                .memberId(board.getMemberId().value())
//                .title(board.getTitle())
//                .content(board.getContent())
//                .category(board.getCategory())
//                .createdAt(board.getCreatedAt())
//                .updatedAt(board.getUpdatedAt())
//                .description(board.getDescription())
//                .build();
//    }
//
//    public Board toDomain() {
//        return new Board(
//                this.id != null ? new BoardId(this.id) : null,
//                new MemberId(this.memberId),
//                this.title,
//                this.content,
//                this.category,
//                this.createdAt,
//                this.updatedAt,
//                this.description
//        );
//    }
}