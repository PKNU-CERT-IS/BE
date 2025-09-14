package org.certis.studyplatform.board.infrastructure.persistence.jpa;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardAttachedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardAttachedJpaRepository extends JpaRepository<BoardAttachedEntity, Long> {

    @Modifying
    @Query("UPDATE BoardAttachedEntity a SET a.deletedAt = CURRENT_TIMESTAMP WHERE a.boardId = :boardId AND a.deletedAt IS NULL")
    void softDeleteByBoardId(@Param("boardId") Long boardId);

    List<BoardAttachedEntity> findByBoardIdAndDeletedAtIsNull(Long boardId);
}
