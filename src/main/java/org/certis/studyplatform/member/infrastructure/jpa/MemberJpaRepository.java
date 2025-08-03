package org.certis.studyplatform.member.infrastructure.jpa;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByStudentNumber(String studentNumber);

    List<MemberEntity> findByRole(String role);

//    @Query("SELECT m FROM MemberEntity m WHERE :skill = ANY(m.skills)")
//    List<MemberEntity> findBySkillsContaining(@Param("skill") String skill);


    // ✅ H2와 PostgreSQL 모두 호환되는 네이티브 쿼리
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

    List<MemberEntity> findByGrade(String grade);

    @Query("SELECT m FROM MemberEntity m WHERE m.name LIKE %:name%")
    List<MemberEntity> findByNameContaining(@Param("name") String name);
}