package org.certis.studyplatform.board.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.object.command.AttachmentCommand;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.DeleteBoardCommand;
import org.certis.studyplatform.board.application.object.command.ToggleLikeCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.domain.model.vo.BoardLikeVo;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardCommandService {

    private final BoardDomainService boardDomainService;
    private final S3FileService s3FileService;

    /**
     * 게시글 생성
     *
     * @param command 게시글 생성 명령
     */
    public void createBoard(CreateBoardCommand command) {
        log.info("Command Service: Creating board - title: {}, author: {}",
                command.title(), command.authorId());

        // 첨부파일 전처리 (data: URL → S3 업로드)
        CreateBoardCommand processed = preprocessCreateCommand(command);

        // Domain Service에 Command 전달
        boardDomainService.createBoard(processed);

        log.info("Command Service: Board created successfully");
    }

    /**
     * 게시글 수정
     *
     * @param command 게시글 수정 명령 (권한 체크 포함)
     */
    public void updateBoard(UpdateBoardCommand command) {
        log.info("Command Service: Updating board - ID: {}, title: {}, requester: {}",
                command.boardId(), command.title(), command.requesterId());

        // 첨부파일 전처리 (data: URL → S3 업로드)
        UpdateBoardCommand processed = preprocessUpdateCommand(command);

        // Domain Service에 Command 전달 (권한 체크 포함)
        boardDomainService.updateBoard(processed);

        log.info("Command Service: Board updated successfully - ID: {}", command.boardId());
    }

    /**
     * 게시글 삭제
     *
     * @param command 게시글 삭제 명령 (권한 체크 포함)
     */
    public void deleteBoard(DeleteBoardCommand command) {
        log.info("Command Service: Deleting board - ID: {}, requester: {}, role: {}",
                command.boardId(), command.requesterId(), command.requesterRole());

        // Domain Service에 Command 전달 (권한 체크 포함)
        boardDomainService.deleteBoard(command);

        log.info("Command Service: Board deleted successfully - ID: {}", command.boardId());
    }

    /**
     * 게시글 좋아요 토글
     *
     * @param command 좋아요 토글 명령
     * @return 좋아요 상태 및 개수
     */
    public BoardLikeVo toggleLike(ToggleLikeCommand command) {
        log.info("Command Service: Toggling like - boardId: {}, memberId: {}",
                command.boardId(), command.memberId());

        // Domain Service에 Command 전달 → VO 반환
        BoardLikeVo result = boardDomainService.toggleLike(command);

        log.info("Command Service: Like toggled - boardId: {}, isLiked: {}, count: {}",
                command.boardId(), result.isLiked(), result.likeCount());

        return result;
    }

    // ================================================================
    // Attachment preprocessing helpers
    // ================================================================

    private CreateBoardCommand preprocessCreateCommand(CreateBoardCommand command) {
        List<AttachmentCommand> processed = preprocessAttachments(command.attachments());
        return CreateBoardCommand.of(
                command.title(),
                command.content(),
                command.description(),
                command.category(),
                command.authorId(),
                processed
        );
    }

    private UpdateBoardCommand preprocessUpdateCommand(UpdateBoardCommand command) {
        List<AttachmentCommand> processed = preprocessAttachments(command.attachments());
        return UpdateBoardCommand.of(
                command.boardId(),
                command.title(),
                command.content(),
                command.description(),
                command.category(),
                command.requesterId(),
                processed
        );
    }

    private List<AttachmentCommand> preprocessAttachments(List<AttachmentCommand> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(this::processAttachment)
                .collect(Collectors.toList());
    }

    private AttachmentCommand processAttachment(AttachmentCommand attachment) {
        String url = attachment.attachedUrl();
        if (url == null || url.isBlank()) {
            return attachment;
        }
        if (url.startsWith("data:")) {
            // data URL: data:<mime>;base64,<payload>
            try {
                int commaIdx = url.indexOf(',');
                String meta = url.substring(5, commaIdx); // strip 'data:'
                String base64Part = url.substring(commaIdx + 1);
                String contentType = meta.contains(";") ? meta.substring(0, meta.indexOf(';')) : "application/octet-stream";
                byte[] bytes = Base64.getDecoder().decode(base64Part.getBytes(StandardCharsets.UTF_8));
                String originalName = attachment.name() != null ? attachment.name() : "board.bin";
                String s3Url = s3FileService.uploadBytes(bytes, contentType, originalName, "board-attachments");
                return AttachmentCommand.of(attachment.id(), attachment.name(), attachment.type(), attachment.size(), s3Url);
            } catch (Exception e) {
                throw new ApplicationException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
            }
        }
        // If already S3 URL or external URL, keep as is
        return attachment;
    }
}