package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.domain.model.vo.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BoardQueryRepository {
    Optional<BoardVo> findById(BoardIdVo boardIdVo);

    Page<BoardSummaryVo> searchBoards(BoardSearchVo searchVo);

    Optional<BoardVo> findByIdWithAttachments(BoardIdVo boardIdVo);

    String getAuthorName(BoardIdVo boardIdVo);
    
    // Get author information with role for board detail
    BoardAuthorInfoVo getAuthorInfo(BoardIdVo boardIdVo);

    Long getAuthorId(BoardIdVo boardIdVo);

    // 모든 활성 게시글 id 반환
    List<Long> findAllActiveBoardIds();

    Long getLikeCountFromDB(BoardIdVo boardIdVo);

    Map<Long, Long> getLikeCountsFromDB(List<BoardIdVo> boardIds);

    Long getViewCountFromDB(BoardIdVo boardIdVo);

    Map<Long, Long> getViewCountsFromDB(List<BoardIdVo> boardIds);

    List<AttachmentVo> findAttachmentsByBoardId(BoardIdVo boardIdVo);

    // 게시글 현재 통계 조회
    BoardStatsVo getBoardStats(BoardIdVo boardIdVo);

    // 특정 사용자가 게시글을 좋아요했는지 DB 기준 확인 (소프트 삭제 제외)
    boolean hasMemberLiked(BoardIdVo boardIdVo, Long memberId);

}
