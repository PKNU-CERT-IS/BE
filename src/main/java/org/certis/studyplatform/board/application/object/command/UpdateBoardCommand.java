package org.certis.studyplatform.board.application.object.command;

import java.util.List;

public record UpdateBoardCommand(
        Long boardId,
        String title,
        String content,
        String description,
        String category,
        Long requesterId,  // 권한 체크용
        List<AttachmentCommand> attachments
) {
    public static UpdateBoardCommand of(Long boardId, String title, String content, String description,
                                        String category, Long requesterId, List<AttachmentCommand> attachments) {
        return new UpdateBoardCommand(boardId, title, content, description, category, requesterId, attachments);
    }
}