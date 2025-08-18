package org.certis.studyplatform.board.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.DeleteBoardCommand;
import org.certis.studyplatform.board.application.object.command.ToggleLikeCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.application.object.query.GetBoardDetailQuery;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.domain.repository.BoardCommandRepository;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardDomainService {

    private final BoardCommandRepository boardCommandRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final BoardRedisRepository boardRedisRepository;

    /**
     * 게시글 생성
     * 1. 게시글 생성 (JPA)
     * 2. 첨부파일 저장
     * 3. Redis 초기화 (좋아요 0, 조회수 0)
     */
    public void createBoard(CreateBoardCommand command) {
        log.info("Domain: Creating board - title: {}, author: {}", command.title(), command.authorId());

        // 1. Command → VO 변환
        List<AttachmentVo> attachments = command.attachments().stream()
                .map(cmd -> AttachmentVo.of(null, cmd.name(), cmd.type(), cmd.size(), cmd.attachedUrl()))
                .toList();

        BoardCreationVo creationVo = BoardCreationVo.of(
                command.title(),
                command.content(),
                command.description(),
                command.category(),
                command.authorId(),
                attachments
        );

        // 2. 게시글 + 첨부파일 저장
        BoardIdVo boardIdVo = boardCommandRepository.createBoard(creationVo);

        // 3. Redis 초기 통계 설정
        boardRedisRepository.initializeStats(boardIdVo);

        log.info("Domain: Board created successfully - ID: {}", boardIdVo.value());
    }

    /**
     * 게시글 수정
     * 1. 권한 체크 (작성자만)
     * 2. 게시글 수정 (JPA)
     * 3. 첨부파일 차등 처리 (삭제할 것 삭제 + 새로운 것 추가)
     */
    public void updateBoard(UpdateBoardCommand command) {
        log.info("Domain: Updating board - ID: {}, requester: {}", command.boardId(), command.requesterId());

        // 1. 게시글 존재 및 권한 확인
        BoardIdVo boardIdVo = BoardIdVo.of(command.boardId());
        BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

        if (!existingBoard.isAuthor(command.requesterId())) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_AUTHOR_MISMATCH);
        }

        // 2. Command → VO 변환
        List<AttachmentVo> attachments = command.attachments().stream()
                .map(cmd -> AttachmentVo.of(cmd.id(), cmd.name(), cmd.type(), cmd.size(), cmd.attachedUrl()))
                .toList();

        BoardUpdateVo updateVo = BoardUpdateVo.of(
                command.boardId(),
                command.title(),
                command.content(),
                command.description(),
                command.category(),
                attachments
        );

        // 3. 게시글 수정 + 첨부파일 차등 처리
        boardCommandRepository.updateBoard(updateVo, existingBoard);

        log.info("Domain: Board updated successfully - ID: {}", command.boardId());
    }

    /**
     * 게시글 삭제
     * 1. 권한 체크 (작성자 또는 STAFF 이상)
     * 2. Soft Delete (deleted_at 설정)
     * 3. Redis 데이터 삭제
     */
    public void deleteBoard(DeleteBoardCommand command) {
        log.info("Domain: Deleting board - ID: {}, requester: {}", command.boardId(), command.requesterId());

        // 1. 게시글 존재 및 권한 확인
        BoardIdVo boardIdVo = BoardIdVo.of(command.boardId());
        BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

        boolean isAuthor = existingBoard.isAuthor(command.requesterId());

        // 스태프 이상의 권한 혹은 저자여야만 삭제가능
        boolean isStaffOrAbove = MemberRole.isStaffOrAbove(MemberRole.valueOf(command.requesterRole()));

        if (!isAuthor && !isStaffOrAbove) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_DELETE_PERMISSION_DENIED);
        }

        // 2. Soft Delete
        boardCommandRepository.deleteBoard(boardIdVo);

        // 3. Redis 데이터 정리
        boardRedisRepository.deleteStats(boardIdVo);

        log.info("Domain: Board deleted successfully - ID: {}", command.boardId());
    }

    /**
     * 게시글 좋아요 토글
     * 1. 본인 게시글 체크 (본인 좋아요 방지)
     * 2. Redis에서 좋아요 토글 (실패 시 에러 반환)
     * 3. 카운트 조회
     */
    public BoardLikeVo toggleLike(ToggleLikeCommand command) {
        log.info("Domain: Toggling like - boardId: {}, memberId: {}", command.boardId(), command.memberId());

        // 1. 게시글 존재 확인
        BoardIdVo boardIdVo = BoardIdVo.of(command.boardId());
        BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

        // 2. 본인 게시글 좋아요 방지
        if (existingBoard.isAuthor(command.memberId())) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_LIKE_SELF_NOT_ALLOWED);
        }

        // 3. Redis에서 좋아요 토글 (실패 시 에러 반환)
        ToggleLikeVo toggleVo = ToggleLikeVo.of(command.boardId(), command.memberId());
        boolean newLikeStatus = toggleLikeInRedis(toggleVo);

        // 4. 좋아요 수 조회
        Long likeCount = getLikeCountFromRedis(boardIdVo);

        BoardLikeVo result = BoardLikeVo.of(command.boardId(), command.memberId(), newLikeStatus, likeCount);

        log.info("Domain: Like toggled - boardId: {}, isLiked: {}, count: {}",
                command.boardId(), newLikeStatus, likeCount);

        return result;
    }

    // ================================================================
    // QUERY OPERATIONS
    // ================================================================

    /**
     * 게시글 키워드 검색
     * 1. Query → VO 변환 (검증 포함)
     * 2. JOOQ 복합 쿼리 (제목/설명 LIKE + 카테고리 필터)
     * 3. 각 게시글별 Redis 통계 조합 (읽기는 Fallback 유지)
     * 4. 페이징 처리
     */
    public Page<BoardSummaryVo> searchBoards(SearchBoardsQuery query) {
        log.info("Domain: Searching boards - search: {}, category: {}", query.search(), query.category());

        // 1. Query → VO 변환 (검증 포함)
        BoardSearchVo searchVo = BoardSearchVo.of(
                query.search(),
                query.category(),
                query.page(),
                query.size()
        );

        // 2. Repository에 VO 전달하여 기본 검색
        Page<BoardSummaryVo> boards = boardQueryRepository.searchBoards(searchVo);

        // 3. 각 게시글별 Redis 통계 보완 (읽기는 Fallback 유지)
        List<BoardSummaryVo> enrichedBoards = boards.getContent().stream()
                .map(this::enrichBoardWithStatsWithFallback)
                .toList();

        // 4. 통계가 포함된 새로운 Page 객체 생성
        Pageable pageable = PageRequest.of(searchVo.page(), searchVo.size());
        Page<BoardSummaryVo> result = new PageImpl<>(enrichedBoards, pageable, boards.getTotalElements());

        log.info("Domain: Found {} boards", result.getTotalElements());
        return result;
    }

    /**
     * 게시글 상세 조회
     * 1. 게시글 조회 (JOOQ + 첨부파일 JOIN)
     * 2. Redis에서 조회수 증가 (실패 시 에러 반환)
     */
    public BoardDetailVo getBoardDetail(GetBoardDetailQuery query) {
        log.info("Domain: Getting board detail - ID: {}, viewerId: {}", query.boardId(), query.viewerId());

        // 1. 게시글 존재 확인 및 조회 (첨부파일 포함)
        BoardIdVo boardIdVo = BoardIdVo.of(query.boardId());
        BoardVo board = boardQueryRepository.findByIdWithAttachments(boardIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

        // 2. Redis에서 조회수 증가 (실패 시 에러 반환)
        IncrementViewVo viewVo = IncrementViewVo.of(query.boardId(), query.viewerId());
        incrementViewCountInRedis(viewVo);

        // 3. Redis에서 통계 조회
        Long likeCount = getLikeCountFromRedis(boardIdVo);
        Long viewCount = getViewCountFromRedis(boardIdVo);

        // 4. 현재 사용자 좋아요 상태 확인
        boolean isLikedByCurrentUser = isLikedByMemberInRedis(boardIdVo, query.viewerId());

        // 5. 작성자 정보 조회
        String authorName = boardQueryRepository.getAuthorName(boardIdVo);

        // 6. 통계가 포함된 DetailVo 생성
        BoardDetailVo result = BoardDetailVo.of(board, authorName, likeCount, viewCount, isLikedByCurrentUser);

        log.info("Domain: Board detail retrieved - ID: {}, title: {}", board.id(), board.title());
        return result;
    }

    private boolean isLikedByMemberInRedis(BoardIdVo boardIdVo, Long memberId) {
        try {
            return boardRedisRepository.isLikedByMember(boardIdVo, memberId);
        } catch (Exception e) {
            log.error("Domain: Redis failed for like status check - boardId: {}, memberId: {}",
                    boardIdVo.value(), memberId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    /**
     * 모든 게시글 통계 동기화 (Redis → RDB)
     * 매일 00시 배치 작업에서 호출
     */
    public void syncAllBoardStats() {
        log.info("Domain: Starting board stats sync from Redis to Database");

        // 1. 모든 활성 게시글 ID 조회
        List<Long> activeBoardIds = boardQueryRepository.findAllActiveBoardIds();
        log.info("Domain: Found {} active boards to sync", activeBoardIds.size());

        // 2. 각 게시글별 통계 동기화
        int successCount = 0;
        int failCount = 0;

        for (Long boardId : activeBoardIds) {
            try {
                syncSingleBoardStats(boardId);
                successCount++;
            } catch (Exception e) {
                log.error("Domain: Failed to sync stats for board: {}", boardId, e);
                failCount++;
            }
        }

        log.info("Domain: Board stats sync completed - Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * 단일 게시글 통계 동기화 (Redis → RDB)
     */
    private void syncSingleBoardStats(Long boardId) {
        log.debug("Domain: Syncing stats for board: {}", boardId);

        // 1. VO 변환 jooq 조회
        BoardIdVo boardIdVo = BoardIdVo.of(boardId);
        BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

        // 2. Redis에서 최신 통계 조회
        Long redisLikeCount = boardRedisRepository.getLikeCount(boardIdVo);
        Long redisViewCount = boardRedisRepository.getViewCount(boardIdVo);

        // 3. RDB 업데이트
        boardCommandRepository.updateBoardStats(boardIdVo, redisLikeCount, redisViewCount, existingBoard.authorId());

        log.debug("Domain: Stats synced for board: {} - Likes: {}, Views: {}",
                boardId, redisLikeCount, redisViewCount);
    }

    // ================================================================
    // REDIS OPERATIONS - Redis 전용 (에러 시 예외 발생)
    // ================================================================

    /**
     * Redis에서 조회수 증가 (실패 시 에러 반환)
     */
    private void incrementViewCountInRedis(IncrementViewVo viewVo) {
        try {
            BoardIdVo boardIdVo = BoardIdVo.of(viewVo.boardId());
            // Redis에서 중복 조회 확인 후 증가
            if (!boardRedisRepository.isViewedByMember(boardIdVo, viewVo.viewerId())) {
                boardRedisRepository.addView(boardIdVo, viewVo.viewerId());
                log.debug("Domain: View count increased in Redis for board: {}", viewVo.boardId());
            }
        } catch (Exception e) {
            log.error("Domain: Redis failed for view increment - boardId: {}", viewVo.boardId(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    /**
     * Redis에서 좋아요 토글 (실패 시 에러 반환)
     */
    private boolean toggleLikeInRedis(ToggleLikeVo toggleVo) {
        try {
            BoardIdVo boardIdVo = BoardIdVo.of(toggleVo.boardId());
            // Redis에서 현재 상태 확인 후 토글
            boolean isCurrentlyLiked = boardRedisRepository.isLikedByMember(boardIdVo, toggleVo.memberId());
            boolean newLikeStatus = !isCurrentlyLiked;

            if (newLikeStatus) {
                boardRedisRepository.addLike(boardIdVo, toggleVo.memberId());
            } else {
                boardRedisRepository.removeLike(boardIdVo, toggleVo.memberId());
            }

            log.debug("Domain: Like toggled in Redis - boardId: {}, newStatus: {}", toggleVo.boardId(), newLikeStatus);
            return newLikeStatus;

        } catch (Exception e) {
            log.error("Domain: Redis failed for like toggle - boardId: {}", toggleVo.boardId(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    /**
     * Redis에서 좋아요 수 조회 (실패 시 에러 반환)
     */
    private Long getLikeCountFromRedis(BoardIdVo boardIdVo) {
        try {
            return boardRedisRepository.getLikeCount(boardIdVo);
        } catch (Exception e) {
            log.error("Domain: Redis failed for like count - boardId: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    /**
     * Redis에서 조회수 조회 (실패 시 에러 반환)
     */
    private Long getViewCountFromRedis(BoardIdVo boardIdVo) {
        try {
            return boardRedisRepository.getViewCount(boardIdVo);
        } catch (Exception e) {
            log.error("Domain: Redis failed for view count - boardId: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    // ================================================================
    // READ OPERATIONS - 읽기는 Fallback 유지 (조회는 실패해도 서비스 가능)
    // ================================================================

    /**
     * 게시글에 통계 정보 보완 (읽기는 Fallback 유지)
     */
    private BoardSummaryVo enrichBoardWithStatsWithFallback(BoardSummaryVo board) {

        BoardIdVo boardIdVo = BoardIdVo.of(board.id());

        Long likeCount = getLikeCountFromRedis(boardIdVo);
        Long viewCount = getViewCountFromRedis(boardIdVo);

        // 새로운 BoardSummaryVo 생성 (통계 포함)
        return BoardSummaryVo.of(
                board.id(),
                board.title(),
                board.description(),
                board.category(),
                board.authorId(),
                board.authorName(),
                board.updatedAt(),
                likeCount,
                viewCount
        );
    }

    /**
     * Redis ↔ RDB 데이터 일관성 검증
     */
    public boolean validateStatsConsistency() {
        log.info("Domain: Starting board stats consistency validation");

        List<Long> activeBoardIds = boardQueryRepository.findAllActiveBoardIds();
        int inconsistentCount = 0;

        for (Long boardId : activeBoardIds) {
            try {
                BoardIdVo boardIdVo = BoardIdVo.of(boardId);

                // Redis 데이터
                Long redisLikeCount = boardRedisRepository.getLikeCount(boardIdVo);
                Long redisViewCount = boardRedisRepository.getViewCount(boardIdVo);

                // RDB 데이터
                Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
                Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);

                // 일관성 체크
                boolean likeConsistent = redisLikeCount.equals(dbLikeCount);
                boolean viewConsistent = redisViewCount.equals(dbViewCount);

                if (!likeConsistent || !viewConsistent) {
                    log.warn("Domain: Data inconsistency found for board: {} - " +
                                    "Redis(like:{}, view:{}) vs DB(like:{}, view:{})",
                            boardId, redisLikeCount, redisViewCount, dbLikeCount, dbViewCount);
                    inconsistentCount++;
                }

            } catch (Exception e) {
                log.error("Domain: Failed to validate consistency for board: {}", boardId, e);
                inconsistentCount++;
            }
        }

        boolean isConsistent = inconsistentCount == 0;
        log.info("Domain: Consistency validation completed - Inconsistent boards: {}", inconsistentCount);

        return isConsistent;
    }
}