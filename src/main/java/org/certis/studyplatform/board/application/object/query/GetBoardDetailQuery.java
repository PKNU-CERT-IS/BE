package org.certis.studyplatform.board.application.object.query;

public record GetBoardDetailQuery(
        Long boardId,
        Long viewerId  // 조회하는 사용자 ID (조회수 증가 + 좋아요 상태 확인용)
) {
    public static GetBoardDetailQuery of(Long boardId, Long viewerId) {
        return new GetBoardDetailQuery(boardId, viewerId);
    }
}
