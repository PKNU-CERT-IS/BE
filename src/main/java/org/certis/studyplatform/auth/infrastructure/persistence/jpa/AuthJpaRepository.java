package org.certis.studyplatform.auth.infrastructure.persistence.jpa;

import org.certis.studyplatform.auth.infrastructure.persistence.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthJpaRepository extends JpaRepository<AuthEntity, Long> {
}
