package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Study Attached JPA Repository
 *
 * StudyAttachedEntity에 대한 JPA Repository
 * Infrastructure Layer
 */
@Repository
public interface StudyAttachedJpaRepository extends JpaRepository<StudyAttachedEntity, Long> {
    List<StudyAttachedEntity> findByStudyId(Long studyId);

    @Modifying
    @Query("DELETE FROM StudyAttachedEntity s WHERE s.studyId = :studyId")
    void deleteByStudyId(@Param("studyId") Long studyId);
} 