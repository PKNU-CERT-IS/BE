package org.certis.studyplatform.schedule.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.schedule.application.object.command.*;
import org.certis.studyplatform.schedule.domain.service.ScheduleDomainService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleCommandService {

    private final ScheduleDomainService scheduleDomainService;

    /**
     * 동방 사용 요청 생성
     * 스케줄 생성 + 스케줄상태(PENDING) 함께 생성
     */
    public void createClubRoomUsage(CreateClubRoomUsageCommand command) {
        log.info("Command Service: Creating club room usage for member ID: {}", command.memberId());

        scheduleDomainService.createClubRoomUsage(command);

        log.info("Command Service: Club room usage created successfully for member ID: {}", command.memberId());
    }

    /**
     * 동방 사용 요청 삭제
     * 스케줄상태 삭제 + 스케줄 삭제 (Hard Delete)
     */
    public void deleteClubRoomUsage(DeleteClubRoomUsageCommand command) {
        log.info("Command Service: Deleting club room usage for member ID: {}, schedule ID: {}",
                command.memberId(), command.scheduleId());

        // 도메인 서비스에서 해당  권한 검증 (자신의 요청만 삭제 가능)
        scheduleDomainService.validateDeletePermission(command);

        // Domain Service 호출 - 연관 데이터 함께 삭제
        scheduleDomainService.deleteClubRoomUsage(command);

        log.info("Command Service: Club room usage deleted successfully for member ID: {}", command.memberId());
    }

    /**
     * 관리자 스케줄 생성
     * 관리자가 직접 생성하는 스케줄 (상태: APPROVED)
     */
    public void createScheduleByAdmin(CreateScheduleByAdminCommand command) {
        log.info("Command Service: Creating schedule by admin ID: {}", command.adminId());

        // Domain Service 호출
        scheduleDomainService.createScheduleByAdmin(command);

        log.info("Command Service: Schedule created successfully by admin ID: {}", command.adminId());
    }

    /**
     * 스케줄 요청 승인/거절
     * 스케줄상태를 PENDING → APPROVED/REJECTED로 변경
     */
    public void approveOrRejectScheduleRequest(ApproveOrRejectScheduleRequestCommand command) {
        log.info("Command Service: Processing schedule request ID: {}, status: {}",
                command.scheduleId(), command.status());

        // Domain Service 호출
        scheduleDomainService.approveOrRejectScheduleRequest(command);

        log.info("Command Service: Schedule request processed successfully ID: {}", command.scheduleId());
    }

    /**
     * 스케줄 삭제 (어드민용)
     * 모든 상태의 스케줄 삭제 가능
     */
    public void deleteSchedule(DeleteScheduleCommand command) {
        log.info("Command Service: Deleting schedule ID: {} by admin ID: {}",
                command.scheduleId(), command.adminId());

        // Domain Service 호출
        scheduleDomainService.deleteSchedule(command);

        log.info("Command Service: Schedule deleted successfully ID: {}", command.scheduleId());
    }
}
