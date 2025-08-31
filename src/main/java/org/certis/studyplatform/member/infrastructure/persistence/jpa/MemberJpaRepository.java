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

    // Basic finders
    Optional<MemberEntity> findByStudentNumber(String studentNumber);

    List<MemberEntity> findByRole(MemberRole role);

    List<MemberEntity> findByGrade(String grade);

    @Query("SELECT m FROM MemberEntity m WHERE m.name LIKE %:name%")
    List<MemberEntity> findByNameContaining(@Param("name") String name);

    // Skills 검색 (H2와 PostgreSQL 호환)
    @Query(value = """
        SELECT * FROM member m
        WHERE CASE
            WHEN :skill IS NULL THEN TRUE
            ELSE (
                CASE
                    WHEN m.skills IS NULL THEN FALSE
                    ELSE CAST(m.skills AS TEXT) LIKE CONCAT('%', :skill, '%')
                END
            )
        END
        AND m.deleted_at IS NULL
        """, nativeQuery = true)
    List<MemberEntity> findBySkillsContaining(@Param("skill") String skill);

    // Command Repository를 위한 존재성 확인 메서드들
    boolean existsByStudentNumber(String studentNumber);

    // 역할별, 학년별 카운트를 위한 메서드들
    long countByRole(MemberRole role);

    long countByGrade(String grade);
}