package org.certis.studyplatform.schedule.infrastructure.persistence;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleStatusJpaRepository extends JpaRepository<ScheduleStatusEntity, Long>  {

    // 스케줄 id로 상태 테이블 삭제
    @Modifying
    @Query("DELETE FROM ScheduleStatusEntity s WHERE s.scheduleId = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
