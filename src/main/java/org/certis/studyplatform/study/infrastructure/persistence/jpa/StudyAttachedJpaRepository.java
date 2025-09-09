package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Study Attached JPA Repository
 *
 * StudyAttachedEntity에 대한 JPA Repository
 * Infrastructure Layer
 */
@Repository
public interface StudyAttachedJpaRepository extends JpaRepository<StudyAttachedEntity, Long> {

} 