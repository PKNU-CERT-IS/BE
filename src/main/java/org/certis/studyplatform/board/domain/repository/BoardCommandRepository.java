package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.domain.model.vo.*;

public interface BoardCommandRepository  {

    // 생성된 게시글 ID
    BoardIdVo createBoard(BoardCreationVo creationVo);

    // 게시글 수정
    void updateBoard(BoardUpdateVo updateVo, BoardVo existingBoard);

    // 게시글 삭제
    void deleteBoard(BoardIdVo boardIdVo);

    // 게시글 통계 업데이트 (Redis → RDB 동기화)
    void updateBoardStats(BoardStatsUpdateVo statsUpdateVo);
}
