package org.certis.studyplatform.board.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.DeleteBoardCommand;
import org.certis.studyplatform.board.application.object.command.ToggleLikeCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.domain.model.vo.BoardLikeVo;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardCommandService {

    private final BoardDomainService boardDomainService;

    /**
     * 게시글 생성
     *
     * @param command 게시글 생성 명령
     */
    public void createBoard(CreateBoardCommand command) {
        log.info("Command Service: Creating board - title: {}, author: {}",
                command.title(), command.authorId());

        // Domain Service에 Command 전달
        boardDomainService.createBoard(command);

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

        // Domain Service에 Command 전달 (권한 체크 포함)
        boardDomainService.updateBoard(command);

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
}