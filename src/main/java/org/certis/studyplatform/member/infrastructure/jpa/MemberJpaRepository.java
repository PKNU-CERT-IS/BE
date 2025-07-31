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

    @Query("SELECT m FROM MemberEntity m WHERE :skill = ANY(m.skills)")
    List<MemberEntity> findBySkillsContaining(@Param("skill") String skill);

    List<MemberEntity> findByGrade(String grade);

    @Query("SELECT m FROM MemberEntity m WHERE m.name LIKE %:name%")
    List<MemberEntity> findByNameContaining(@Param("name") String name);
}