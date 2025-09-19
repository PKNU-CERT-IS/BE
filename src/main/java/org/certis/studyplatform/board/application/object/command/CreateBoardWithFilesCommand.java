package org.certis.studyplatform.board.application.object.command;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 업로드를 포함한 게시글 생성 명령
 * 도메인 계층에서 S3 업로드를 처리하기 위해 MultipartFile을 포함
 */
public record CreateBoardWithFilesCommand(
        String title,
        String content,
        String description,
        String category,
        Long authorId,
        List<MultipartFile> attachmentFiles
) {
    public static CreateBoardWithFilesCommand of(String title, String content, String description,
                                                 String category, Long authorId, List<MultipartFile> attachmentFiles) {
        return new CreateBoardWithFilesCommand(title, content, description, category, authorId, attachmentFiles);
    }
}
