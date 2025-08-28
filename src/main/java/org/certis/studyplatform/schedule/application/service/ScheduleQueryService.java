package org.certis.studyplatform.schedule.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.schedule.application.object.query.GetAllApprovedScheduleRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetMyRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetPendingScheduleRequestsQuery;
import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.domain.service.ScheduleDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduleQueryService {

    private final ScheduleDomainService scheduleDomainService;


    /**
     * 모든 승인된 스케줄 조회 (월별, 캘린더용)
     * APPROVED 상태의 스케줄만 조회
     */
    public List<ScheduleVo> getAllApprovedScheduleRequests(GetAllApprovedScheduleRequestsQuery query) {
        log.info("Query Service: Getting all approved schedule requests for date: {}", query.date());

        List<ScheduleVo> scheduleVos = scheduleDomainService.getAllApprovedScheduleRequests(query);

        log.info("Query Service: Found {} approved schedule requests", scheduleVos.size());
        return scheduleVos;
    }

    /**
     * 내가 요청한 모든 스케줄 조회 (전체)
     * 승인/거절/대기 모든 상태 포함
     */
    public List<ScheduleVo> getMyRequests(GetMyRequestsQuery query) {
        log.info("Query Service: Getting my requests for member ID: {}", query.memberId());

        // Domain Service 호출
        List<ScheduleVo> scheduleVos = scheduleDomainService.getMyRequests(query);

        log.info("Query Service: Found {} my requests for member ID: {}",
                scheduleVos.size(), query.memberId());

        return scheduleVos;
    }

    /**
     * 어드민용 대기중인 스케줄 요청 조회
     * PENDING 상태의 스케줄만 조회 + 회원정보 포함
     */
    public List<AdminScheduleVo> getPendingScheduleRequests(GetPendingScheduleRequestsQuery query) {
        log.info("Query Service: Getting pending schedule requests for admin ID: {}", query.adminId());

        // Domain Service 호출 - 회원정보와 함께 조회
        List<AdminScheduleVo> scheduleVos = scheduleDomainService.getPendingScheduleRequests(query);

        log.info("Query Service: Found {} pending schedule requests", scheduleVos.size());

        return scheduleVos;
    }
}
