package org.certis.studyplatform.schedule.infrastructure.persistence.jpa;

import org.certis.studyplatform.schedule.infrastructure.persistence.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {
    // JPA 기본 CRUD 메서드만 사용
    // 읽기 작업은 jOOQ로 처리
}
