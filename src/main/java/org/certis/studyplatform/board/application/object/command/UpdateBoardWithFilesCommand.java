package org.certis.studyplatform.board.application.object.command;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 업로드를 포함한 게시글 수정 명령
 * 도메인 계층에서 S3 업로드를 처리하기 위해 MultipartFile을 포함
 */
public record UpdateBoardWithFilesCommand(
        Long boardId,
        String title,
        String content,
        String description,
        String category,
        Long requesterId,
        List<String> existingAttachmentUrls, // 유지할 기존 첨부파일 URL들
        List<MultipartFile> newAttachmentFiles // 새로 추가할 첨부파일들
) {
    public static UpdateBoardWithFilesCommand of(Long boardId, String title, String content, String description,
                                                 String category, Long requesterId, 
                                                 List<String> existingAttachmentUrls, List<MultipartFile> newAttachmentFiles) {
        return new UpdateBoardWithFilesCommand(boardId, title, content, description, category, requesterId, 
                                               existingAttachmentUrls, newAttachmentFiles);
    }
}
