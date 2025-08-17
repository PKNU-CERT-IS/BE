package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardViewJpaRepository extends JpaRepository<BoardViewEntity, Long> {
}
