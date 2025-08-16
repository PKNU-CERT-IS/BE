package org.certis.studyplatform.board.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.mapper.BoardApplicationMapper;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.DeleteBoardCommand;
import org.certis.studyplatform.board.application.object.command.ToggleLikeCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.application.object.query.GetBoardDetailQuery;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.domain.model.vo.BoardLikeVo;
import org.certis.studyplatform.board.domain.model.vo.BoardSummaryVo;
import org.certis.studyplatform.board.domain.model.vo.BoardVo;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardSearchRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardDetailResponseDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardLikeResponseDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardFacadeService {

    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    private final BoardApplicationMapper boardApplicationMapper;

    /**
     * 게시글 키워드 검색 조회
     * @param request 검색 조건 (Query Parameters)
     * @return 페이징된 게시글 목록
     */
    public Page<BoardListResponseDto> searchBoards(BoardSearchRequestDto request) {
        log.info("Facade: Searching boards - search: {}, category: {}, page: {}, size: {}",
                request.getSearch(), request.getCategory(), request.getPage(), request.getSize());

        // 1. DTO → Query Object 변환
        SearchBoardsQuery query = boardApplicationMapper.toSearchBoardsQuery(request);

        // 2. Query Service 호출 → VO 반환
        Page<BoardSummaryVo> boardSummaryVos = boardQueryService.searchBoards(query);

        // 3. VO → ResponseDTO 변환
        Page<BoardListResponseDto> result = boardApplicationMapper.toBoardListResponseDtoPage(boardSummaryVos);

        log.info("Facade: Found {} boards", result.getTotalElements());
        return result;
    }

    /**
     * 게시글 상세 조회
     *
     * @param boardId 게시글 ID
     * @param memberId 조회하는 사용자 ID (조회수 증가 + 좋아요 상태 확인)
     * @return 게시글 상세 정보
     */
    public BoardDetailResponseDto getBoardDetail(Long boardId, Long memberId) {
        log.info("Facade: Getting board detail - ID: {}, viewerId: {}", boardId, memberId);

        // 1. Parameters → Query Object 변환
        GetBoardDetailQuery query = GetBoardDetailQuery.of(boardId, memberId);

        // 2. Query Service 호출 → VO 반환 (조회수 증가 포함)
        BoardVo boardVo = boardQueryService.getBoardDetail(query);

        // 3. VO → ResponseDTO 변환 (좋아요 상태 포함)
        BoardDetailResponseDto result = boardApplicationMapper.toBoardDetailResponseDto(boardVo, memberId);

        log.info("Facade: Board detail retrieved - ID: {}", boardId);
        return result;
    }

    // ================================================================
    // COMMAND OPERATIONS - 상태 변경 작업
    // ================================================================

    /**
     * 게시글 생성
     *
     * @param request 게시글 생성 요청
     * @param memberId 작성자 ID
     */
    @Transactional
    public void createBoard(BoardCreateRequestDto request, Long memberId) {
        log.info("Facade: Creating board - title: {}, author: {}", request.getTitle(), memberId);

        // 1. DTO → Command Object 변환
        CreateBoardCommand command = boardApplicationMapper.toCreateBoardCommand(request, memberId);

        // 2. Command Service 호출
        boardCommandService.createBoard(command);

        log.info("Facade: Board created successfully");
    }

    /**
     * 게시글 수정
     *
     * @param boardId 게시글 ID
     * @param request 게시글 수정 요청
     * @param memberId 요청자 ID (권한 체크용)
     */
    @Transactional
    public void updateBoard(Long boardId, BoardUpdateRequestDto request, Long memberId) {
        log.info("Facade: Updating board - ID: {}, title: {}, requesterId: {}",
                boardId, request.getTitle(), memberId);

        // 1. DTO → Command Object 변환
        UpdateBoardCommand command = boardApplicationMapper.toUpdateBoardCommand(boardId, request, memberId);

        // 2. Command Service 호출 (권한 체크 포함)
        boardCommandService.updateBoard(command);

        log.info("Facade: Board updated successfully - ID: {}", boardId);
    }

    /**
     * 게시글 삭제
     *
     * @param boardId 게시글 ID
     * @param memberId 요청자 ID
     * @param memberRole 요청자 역할 (권한 체크용)
     */
    @Transactional
    public void deleteBoard(Long boardId, Long memberId, String memberRole) {
        log.info("Facade: Deleting board - ID: {}, requesterId: {}, role: {}",
                boardId, memberId, memberRole);

        // 1. Parameters → Command Object 변환
        DeleteBoardCommand command = DeleteBoardCommand.of(boardId, memberId, memberRole);

        // 2. Command Service 호출 (권한 체크 포함)
        boardCommandService.deleteBoard(command);

        log.info("Facade: Board deleted successfully - ID: {}", boardId);
    }

    /**
     * 게시글 좋아요 토글
     *
     * @param boardId 게시글 ID
     * @param memberId 사용자 ID
     * @return 좋아요 상태 및 개수
     */
    @Transactional
    public BoardLikeResponseDto toggleLike(Long boardId, Long memberId) {
        log.info("Facade: Toggling like for board - ID: {}, memberId: {}", boardId, memberId);

        // 1. Parameters → Command Object 변환
        ToggleLikeCommand command = ToggleLikeCommand.of(boardId, memberId);

        // 2. Command Service 호출 → VO 반환
        BoardLikeVo boardLikeVo = boardCommandService.toggleLike(command);

        // 3. VO → ResponseDTO 변환
        BoardLikeResponseDto result = boardApplicationMapper.toBoardLikeResponseDto(boardLikeVo);

        log.info("Facade: Like toggled - ID: {}, isLiked: {}, count: {}",
                boardId, result.isLiked(), result.getLikeCount());
        return result;
    }
}