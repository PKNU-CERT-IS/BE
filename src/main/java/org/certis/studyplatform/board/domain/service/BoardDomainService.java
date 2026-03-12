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

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID));

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
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID));

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
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID));

        // 2. 본인 게시글 좋아요 방지
        if (existingBoard.isAuthor(command.memberId())) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_LIKE_SELF_NOT_ALLOWED);
        }

        // 3. Redis에서 좋아요 토글 (실패 시 에러 반환)
        ToggleLikeVo toggleVo = ToggleLikeVo.of(command.boardId(), command.memberId());
        // Redis 선동기화: DB에 사용자가 이미 좋아요한 기록이 있으면 Redis에 먼저 반영
        if (!boardRedisRepository.isLikedByMember(boardIdVo, command.memberId())
                && boardQueryRepository.hasMemberLiked(boardIdVo, command.memberId())) {
            boardRedisRepository.addLike(boardIdVo, command.memberId());
        }
        boolean newLikeStatus = toggleLikeInRedis(toggleVo);

        // 4. 사용자에게 노출되는 총 좋아요 수 조회
        Long likeCount = getDisplayLikeCount(boardIdVo);

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

        // 3. 각 게시글별 표시 통계 보완 (RDB total + Redis delta)
        List<BoardSummaryVo> enrichedBoards = enrichBoardsWithDisplayStats(boards.getContent());

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
                .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID));

        // 2. Redis에서 조회수 증가 (실패 시 에러 반환)
        IncrementViewVo viewVo = IncrementViewVo.of(query.boardId(), query.viewerId());
        incrementViewCountInRedis(viewVo);

        // 3. 표시용 통계 조회 (RDB total + Redis delta)
        Long likeCount = getDisplayLikeCount(boardIdVo);
        Long viewCount = getDisplayViewCount(boardIdVo);

        // 4. 현재 사용자 좋아요 상태 확인
        boolean isLikedByCurrentUser = isLikedByMemberInRedis(boardIdVo, query.viewerId());

        // 5. 작성자 정보 조회 (이름과 역할)
        BoardAuthorInfoVo authorInfo = boardQueryRepository.getAuthorInfo(boardIdVo);

        // 6. 통계가 포함된 DetailVo 생성
        BoardDetailVo result = BoardDetailVo.of(board, authorInfo.name(), authorInfo.role(), authorInfo.profileImageUrl(), likeCount, viewCount, isLikedByCurrentUser);

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
     * @return 동기화된 게시글 수
     */
    public int syncAllBoardStats() {
        log.info("Domain: Starting board stats sync from Redis to Database");

        // 1. 모든 활성 게시글 ID 조회
        List<Long> activeBoardIds = boardQueryRepository.findAllActiveBoardIds();
        log.info("Domain: Found {} active boards to sync", activeBoardIds.size());
        return syncBoardStats(activeBoardIds);
    }

    public int syncBoardStats(List<Long> boardIds) {
        // 2. 각 게시글별 통계 동기화
        int successCount = 0;
        int failCount = 0;

        for (Long boardId : boardIds) {
            try {
                syncSingleBoardStats(boardId);
                successCount++;
            } catch (Exception e) {
                log.error("Domain: Failed to sync stats for board: {}", boardId, e);
                failCount++;
            }
        }

        log.info("Domain: Board stats sync completed - Success: {}, Failed: {}", successCount, failCount);

        return successCount;
    }

    /**
     * 단일 게시글 통계 동기화 (Redis → RDB)
     * 누적 방식으로 동기화하여 데이터 손실 방지
     */
    private void syncSingleBoardStats(Long boardId) {
        log.debug("Domain: Syncing stats for board: {}", boardId);

        try {
            BoardIdVo boardIdVo = BoardIdVo.of(boardId);
            Long authorId = boardQueryRepository.getAuthorId(boardIdVo);
            if (authorId == null) {
                throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND);
            }

            // 2. active delta를 flush delta로 회전
            BoardRedisDeltaVo rotatedDelta = boardRedisRepository.rotateActiveDeltaToFlush(boardIdVo);

            if (rotatedDelta.flushLikeCount() == 0L && rotatedDelta.flushViewCount() == 0L) {
                log.debug("Domain: No Redis delta to sync for board: {} - skipping", boardId);
                return;
            }

            // 3. 기존 통계 조회 (JOOQ - Query Repository)
            BoardStatsVo currentStats = boardQueryRepository.getBoardStats(boardIdVo);

            // 4. flush bucket 전체를 RDB total에 반영
            Long finalLikeCount = currentStats.likeCount() + rotatedDelta.flushLikeCount();
            Long finalViewCount = currentStats.viewCount() + rotatedDelta.flushViewCount();

            // 6. 통계 업데이트 VO 생성 (누적 방식)
            BoardStatsUpdateVo statsUpdateVo = BoardStatsUpdateVo.of(
                    boardIdVo.value(),
                    authorId,
                    finalLikeCount,
                    finalViewCount,
                    currentStats.likeCount(),
                    currentStats.viewCount(),
                    currentStats.likeId(),   // 기존 Like Entity ID
                    currentStats.viewId()    // 기존 View Entity ID
            );

            // 7. Command Repository로 업데이트 (JPA)
            boardCommandRepository.updateBoardStats(statsUpdateVo);
            boardRedisRepository.clearFlushStats(boardIdVo);

            log.info("Domain: Stats synced for board: {} - Likes: {} → {} (flush delta: {}), Views: {} → {} (flush delta: {})",
                    boardId, currentStats.likeCount(), finalLikeCount, rotatedDelta.flushLikeCount(),
                    currentStats.viewCount(), finalViewCount, rotatedDelta.flushViewCount());

        } catch (Exception e) {
            log.error("Domain: Failed to sync stats for board: {}", boardId, e);
            throw e; // 상위에서 처리하도록 재발생
        }
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

    private Long getDisplayLikeCount(BoardIdVo boardIdVo) {
        try {
            return boardQueryRepository.getLikeCountFromDB(boardIdVo) + boardRedisRepository.getLikeCount(boardIdVo);
        } catch (Exception e) {
            log.error("Domain: Failed to calculate display like count - boardId: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    private Long getDisplayViewCount(BoardIdVo boardIdVo) {
        try {
            return boardQueryRepository.getViewCountFromDB(boardIdVo) + boardRedisRepository.getViewCount(boardIdVo);
        } catch (Exception e) {
            log.error("Domain: Failed to calculate display view count - boardId: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    // ================================================================
    // READ OPERATIONS - 읽기 경로는 DB total + Redis delta
    // ================================================================

    private List<BoardSummaryVo> enrichBoardsWithDisplayStats(List<BoardSummaryVo> boards) {
        if (boards.isEmpty()) {
            return boards;
        }

        List<BoardIdVo> boardIds = boards.stream()
                .map(board -> BoardIdVo.of(board.id()))
                .toList();

        Map<Long, Long> likeDeltas;
        Map<Long, Long> viewDeltas;
        try {
            likeDeltas = boardRedisRepository.getLikeCounts(boardIds);
            viewDeltas = boardRedisRepository.getViewCounts(boardIds);
        } catch (Exception e) {
            log.error("Domain: Failed to batch read Redis deltas for boards", e);
            likeDeltas = Collections.emptyMap();
            viewDeltas = Collections.emptyMap();
        }

        final Map<Long, Long> finalLikeDeltas = likeDeltas;
        final Map<Long, Long> finalViewDeltas = viewDeltas;
        final Map<Long, Long> dbLikeCounts = boardQueryRepository.getLikeCountsFromDB(boardIds);
        final Map<Long, Long> dbViewCounts = boardQueryRepository.getViewCountsFromDB(boardIds);

        return boards.stream()
                .map(board -> BoardSummaryVo.of(
                        board.id(),
                        board.title(),
                        board.description(),
                        board.category() != null ? board.category().value() : null,
                        board.authorId(),
                        board.authorName(),
                        board.updatedAt(),
                        dbLikeCounts.getOrDefault(board.id(), 0L) + finalLikeDeltas.getOrDefault(board.id(), 0L),
                        dbViewCounts.getOrDefault(board.id(), 0L) + finalViewDeltas.getOrDefault(board.id(), 0L)
                ))
                .toList();
    }

    /**
     * 표시값 기준 데이터 일관성 검증
     */
    public boolean validateStatsConsistency() {
        log.info("Domain: Starting board stats consistency validation");

        List<Long> activeBoardIds = boardQueryRepository.findAllActiveBoardIds();
        int inconsistentCount = 0;

        for (Long boardId : activeBoardIds) {
            try {
                BoardIdVo boardIdVo = BoardIdVo.of(boardId);
                Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
                Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
                BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardIdVo);

                Long displayLikeCount = dbLikeCount + delta.totalLikeDelta();
                Long displayViewCount = dbViewCount + delta.totalViewDelta();

                if (displayLikeCount < dbLikeCount || displayViewCount < dbViewCount) {
                    inconsistentCount++;
                    log.warn("Domain: Invalid board stats detected - boardId: {}, dbLike: {}, dbView: {}, delta: {}",
                            boardId, dbLikeCount, dbViewCount, delta);
                }
            } catch (Exception e) {
                log.error("Domain: Failed to validate consistency for board: {}", boardId, e);
                inconsistentCount++;
            }
        }

        boolean isConsistent = inconsistentCount == 0;
        log.info("Domain: Consistency validation completed - Inconsistent: {}, Total: {}",
                inconsistentCount, activeBoardIds.size());

        return isConsistent;
    }

}
