package org.certis.studyplatform.board.domain.model.vo;

import java.time.OffsetDateTime;

public record BoardSummaryVo(
        Long id,
        String title,
        String description,
        String category,
        Long authorId,
        String authorName,
        OffsetDateTime updatedAt,
        Long likeCount,
        Long viewCount
) {
    public static BoardSummaryVo of(Long id, String title, String description, String category,
                                    Long authorId, String authorName, OffsetDateTime updatedAt,
                                    Long likeCount, Long viewCount) {
        // 필수 필드 검증
        BoardIdVo.of(id);
        BoardTitleVo.of(title);
        BoardDescriptionVo.of(description);
        BoardCategoryVo.of(category);

        return new BoardSummaryVo(id, title, description, category, authorId, authorName,
                updatedAt, likeCount != null ? likeCount : 0L,
                viewCount != null ? viewCount : 0L);
    }
}
