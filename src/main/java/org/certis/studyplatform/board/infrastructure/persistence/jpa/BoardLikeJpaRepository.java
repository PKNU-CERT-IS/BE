package org.certis.studyplatform.board.infrastructure.persistence.jpa;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BoardLikeJpaRepository extends JpaRepository<BoardLikeEntity, Long> {
    @Query("SELECT bl FROM BoardLikeEntity bl WHERE bl.boardId = :boardId ORDER BY bl.updatedAt DESC LIMIT 1")
    BoardLikeEntity findLatestByBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Transactional 
    @Query("UPDATE BoardLikeEntity bl SET bl.likeNumber = :likeNumber WHERE bl.id = :id")
    int updateLikeNumber(@Param("id") Long id, @Param("likeNumber") Integer likeNumber);
}
