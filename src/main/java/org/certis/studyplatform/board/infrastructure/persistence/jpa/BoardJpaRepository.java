package org.certis.studyplatform.board.infrastructure.persistence.jpa;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardJpaRepository extends JpaRepository<BoardEntity, Long> {
}
