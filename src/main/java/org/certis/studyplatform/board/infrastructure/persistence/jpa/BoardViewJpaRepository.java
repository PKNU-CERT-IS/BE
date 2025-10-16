package org.certis.studyplatform.board.infrastructure.persistence.jpa;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BoardViewJpaRepository extends JpaRepository<BoardViewEntity, Long> {
    @Query("SELECT bv FROM BoardViewEntity bv WHERE bv.boardId = :boardId ORDER BY bv.updatedAt DESC LIMIT 1")
    BoardViewEntity findLatestByBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Transactional 
    @Query("UPDATE BoardViewEntity bv SET bv.viewNumber = :viewNumber WHERE bv.id = :id")
    int updateViewNumber(@Param("id") Long id, @Param("viewNumber") Integer viewNumber);
}

