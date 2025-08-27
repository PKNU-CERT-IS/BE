package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardViewJpaRepository extends JpaRepository<BoardViewEntity, Long> {
    @Query("SELECT bv FROM BoardViewEntity bv WHERE bv.boardId = :boardId ORDER BY bv.updatedAt DESC LIMIT 1")
    BoardViewEntity findLatestByBoardId(@Param("boardId") Long boardId);}

