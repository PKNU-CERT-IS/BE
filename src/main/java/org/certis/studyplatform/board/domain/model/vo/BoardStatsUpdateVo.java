package org.certis.studyplatform.board.domain.model.vo;

public record BoardStatsUpdateVo(
        Long boardId,
        Long authorId,
        Long newLikeCount,
        Long newViewCount,
        Long currentLikeCount,
        Long currentViewCount,
        Long likeEntityId,    // 기존 Like Entity ID (null이면 새로 생성)
        Long viewEntityId     // 기존 View Entity ID (null이면 새로 생성)
) {
    public static BoardStatsUpdateVo of(Long boardId, Long authorId,
                                        Long newLikeCount, Long newViewCount,
                                        Long currentLikeCount, Long currentViewCount,
                                        Long likeEntityId, Long viewEntityId) {
        return new BoardStatsUpdateVo(boardId, authorId, newLikeCount, newViewCount,
                currentLikeCount, currentViewCount,
                likeEntityId, viewEntityId);
    }

    public boolean isLikeCountChanged() {
        return !newLikeCount.equals(currentLikeCount);
    }

    public boolean isViewCountChanged() {
        return !newViewCount.equals(currentViewCount);
    }

    public boolean hasExistingLikeEntity() {
        return likeEntityId != null;
    }

    public boolean hasExistingViewEntity() {
        return viewEntityId != null;
    }
}