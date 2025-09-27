package org.certis.studyplatform.member.infrastructure.persistence.jpa;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Member JPA Repository
 *
 * CQRS Command 측면에서 사용하는 JPA Repository
 * 주로 MemberCommandRepository에서 사용
 */
public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    // Command Repository를 위한 존재성 확인 메서드들
    boolean existsByStudentNumber(String studentNumber);
}