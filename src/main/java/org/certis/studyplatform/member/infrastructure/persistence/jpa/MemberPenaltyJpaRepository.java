package org.certis.studyplatform.member.infrastructure.persistence.jpa;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberPenaltyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberPenaltyJpaRepository extends JpaRepository<MemberPenaltyEntity, Long> {
    Optional<MemberPenaltyEntity> findByMemberId(Long value);
}
