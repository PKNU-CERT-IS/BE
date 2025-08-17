package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardLikeJpaRepository extends JpaRepository<BoardLikeEntity, Long> {
}
