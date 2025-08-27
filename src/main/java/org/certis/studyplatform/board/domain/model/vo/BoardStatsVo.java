package org.certis.studyplatform.board.domain.model.vo;

public record BoardStatsVo(
        Long likeCount,
        Long viewCount,
        Long likeId,    // Like Entity ID (업데이트용)
        Long viewId     // View Entity ID (업데이트용)
) {
    public static BoardStatsVo of(Long likeCount, Long viewCount, Long likeId, Long viewId) {
        return new BoardStatsVo(likeCount, viewCount, likeId, viewId);
    }

    public static BoardStatsVo empty() {
        return new BoardStatsVo(0L, 0L, null, null);
    }
}
