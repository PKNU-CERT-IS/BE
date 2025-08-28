package org.certis.studyplatform.schedule.domain.repository;

import org.certis.studyplatform.schedule.domain.model.vo.ScheduleIdVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleStatusVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;

public interface ScheduleCommandRepository {

    // 스케줄 생성
    ScheduleVo createSchedule(ScheduleVo scheduleVo);

    // 스케줄 삭제
    void deleteById(ScheduleIdVo scheduleId);

    // 스케줄 상태 생성
    void createScheduleStatus(ScheduleIdVo scheduleId, ScheduleStatusVo status);

    // 스케줄 상태 업데이트
    void updateScheduleStatus(ScheduleIdVo scheduleId, ScheduleStatusVo status);

    // 스케줄 상태 삭제
    void deleteScheduleStatusByScheduleId(ScheduleIdVo scheduleId);
}
