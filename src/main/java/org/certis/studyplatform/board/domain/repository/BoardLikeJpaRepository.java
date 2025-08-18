package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardLikeJpaRepository extends JpaRepository<BoardLikeEntity, Long> {
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.boardId = :boardId ORDER BY bl.updatedAt DESC LIMIT 1")
    BoardLikeEntity findLatestByBoardId(@Param("boardId") Long boardId);}
