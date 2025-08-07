package org.certis.studyplatform.auth.infrastructure.persistence;

import org.certis.studyplatform.auth.domain.model.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthJpaRepository extends JpaRepository<AuthEntity, Long> {
    Optional<AuthEntity> findByAccountNumber(String accountNumber);
    Optional<AuthEntity> findByMemberId(Long memberId);
    boolean existsByAccountNumber(String accountNumber);
}
