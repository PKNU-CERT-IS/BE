package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardAttachedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAttachedJpaRepository extends JpaRepository<BoardAttachedEntity, Long> {
}
