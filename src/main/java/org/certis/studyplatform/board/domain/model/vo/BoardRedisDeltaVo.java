package org.certis.studyplatform.board.domain.model.vo;

public record BoardRedisDeltaVo(
        Long activeLikeCount,
        Long activeViewCount,
        Long flushLikeCount,
        Long flushViewCount
) {
    public static BoardRedisDeltaVo of(Long activeLikeCount, Long activeViewCount,
                                       Long flushLikeCount, Long flushViewCount) {
        return new BoardRedisDeltaVo(
                activeLikeCount != null ? activeLikeCount : 0L,
                activeViewCount != null ? activeViewCount : 0L,
                flushLikeCount != null ? flushLikeCount : 0L,
                flushViewCount != null ? flushViewCount : 0L
        );
    }

    public Long totalLikeDelta() {
        return activeLikeCount + flushLikeCount;
    }

    public Long totalViewDelta() {
        return activeViewCount + flushViewCount;
    }
}
