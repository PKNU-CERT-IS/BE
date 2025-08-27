package org.certis.studyplatform.board.application.object.command;

public record ToggleLikeCommand(
        Long boardId,
        Long memberId
) {
    public static ToggleLikeCommand of(Long boardId, Long memberId) {
        return new ToggleLikeCommand(boardId, memberId);
    }
}
