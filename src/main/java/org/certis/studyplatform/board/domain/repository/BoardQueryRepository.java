package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.domain.model.vo.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface BoardQueryRepository {
    Optional<BoardVo> findById(BoardIdVo boardIdVo);

    Page<BoardSummaryVo> searchBoards(BoardSearchVo searchVo);

    Optional<BoardVo> findByIdWithAttachments(BoardIdVo boardIdVo);

    String getAuthorName(BoardIdVo boardIdVo);

    // 모든 활성 게시글 id 반환
    List<Long> findAllActiveBoardIds();

    Long getLikeCountFromDB(BoardIdVo boardIdVo);

    Long getViewCountFromDB(BoardIdVo boardIdVo);

    List<AttachmentVo> findAttachmentsByBoardId(BoardIdVo boardIdVo);

}
