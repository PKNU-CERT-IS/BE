package org.certis.studyplatform.board.application.object.command;

public record DeleteBoardCommand(
        Long boardId,
        Long requesterId,  // 권한 체크용 (작성자 또는 STAFF 이상)
        String requesterRole
) {
    public static DeleteBoardCommand of(Long boardId, Long requesterId, String requesterRole) {
        return new DeleteBoardCommand(boardId, requesterId, requesterRole);
    }
}