package org.certis.studyplatform.member.infrastructure.persistence.jpa;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberContactJpaRepository extends JpaRepository<MemberContactEntity, Long> {
}
