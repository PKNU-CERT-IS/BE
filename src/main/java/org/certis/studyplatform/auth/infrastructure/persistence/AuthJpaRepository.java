package org.certis.studyplatform.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthJpaRepository extends JpaRepository<AuthEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);
}
