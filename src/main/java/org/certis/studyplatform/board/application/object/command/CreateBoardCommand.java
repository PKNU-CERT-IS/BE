package org.certis.studyplatform.board.application.object.command;

import java.util.List;

public record CreateBoardCommand(
        String title,
        String content,
        String description,
        String category,
        Long authorId,  // CurrentUser에서 추출
        List<AttachmentCommand> attachments
) {
    public static CreateBoardCommand of(String title, String content, String description,
                                        String category, Long authorId, List<AttachmentCommand> attachments) {
        return new CreateBoardCommand(title, content, description, category, authorId, attachments);
    }
}