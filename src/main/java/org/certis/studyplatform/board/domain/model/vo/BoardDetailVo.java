package org.certis.studyplatform.board.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record BoardDetailVo(
        Long id,
        String title,
        String content,
        String description,
        String category,
        Long authorId,
        String authorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AttachmentVo> attachments,
        Long likeCount,
        Long viewCount,
        boolean isLikedByCurrentUser
) {

    public static BoardDetailVo of(BoardVo board, String authorName, Long likeCount,
                                   Long viewCount, boolean isLikedByCurrentUser) {
        return new BoardDetailVo(
                board.id(),
                board.title(),
                board.content(),
                board.description(),
                board.category(),
                board.authorId(),
                authorName,
                board.createdAt(),
                board.updatedAt(),
                board.attachments(),
                likeCount != null ? likeCount : 0L,
                viewCount != null ? viewCount : 0L,
                isLikedByCurrentUser
        );
    }
}