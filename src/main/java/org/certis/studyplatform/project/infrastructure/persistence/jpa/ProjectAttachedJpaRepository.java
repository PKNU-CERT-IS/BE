package org.certis.studyplatform.project.infrastructure.persistence.jpa;

import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectAttachedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project Attached JPA Repository
 *
 * ProjectAttachedEntity에 대한 JPA Repository
 * Infrastructure Layer
 */
@Repository
public interface ProjectAttachedJpaRepository extends JpaRepository<ProjectAttachedEntity, Long> {

} 