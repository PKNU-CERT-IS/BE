package org.certis.studyplatform.board.domain.model.vo;

public record BoardStatsVo(
        Long boardId,
        Long likeCount,
        Long viewCount
) {

    public static BoardStatsVo of(Long boardId, Long likeCount, Long viewCount) {
        BoardIdVo.of(boardId);

        return new BoardStatsVo(
                boardId,
                likeCount != null ? likeCount : 0L,
                viewCount != null ? viewCount : 0L
        );
    }
}
