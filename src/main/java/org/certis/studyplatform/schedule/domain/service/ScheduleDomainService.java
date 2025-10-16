package org.certis.studyplatform.schedule.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.schedule.application.object.command.*;
import org.certis.studyplatform.schedule.application.object.query.GetAllApprovedScheduleRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetMyRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetPendingScheduleRequestsQuery;
import org.certis.studyplatform.schedule.domain.model.vo.*;
import org.certis.studyplatform.schedule.domain.repository.ScheduleCommandRepository;
import org.certis.studyplatform.schedule.domain.repository.ScheduleQueryRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleDomainService {

    private final ScheduleCommandRepository scheduleCommandRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;

    public void createClubRoomUsage(CreateClubRoomUsageCommand command) {
        log.info("Domain: Creating club room usage for member ID: {}", command.memberId());

        SchedulePlaceVo placeVo = SchedulePlaceVo.clubroom();

        ScheduleVo scheduleVo = createScheduleVo(
                command.memberId(),
                command.title(),
                command.description(),
                command.type(),
                placeVo.value(), // 동방 고정
                command.startedAt(),
                command.endedAt()
        );

        ScheduleVo createdSchedule = scheduleCommandRepository.createSchedule(scheduleVo);

        ScheduleIdVo scheduleId = ScheduleIdVo.of(createdSchedule.id());
        ScheduleStatusVo pendingStatus = ScheduleStatusVo.pending();
        scheduleCommandRepository.createScheduleStatus(scheduleId, pendingStatus);

        log.info("Domain: Club room usage created successfully for member ID: {}", command.memberId());
    }

    public void deleteClubRoomUsage(DeleteClubRoomUsageCommand command) {
        log.info("Domain: Deleting club room usage - member ID: {}, schedule ID: {}",
                command.memberId(), command.scheduleId());

        ScheduleIdVo scheduleId = ScheduleIdVo.of(command.scheduleId());
        MemberIdVo memberId = MemberIdVo.of(command.memberId());

        ScheduleVo schedule = scheduleQueryRepository.findById(scheduleId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_NOT_FOUND));

        // 생성자(member_id)와 요청자 비교
        if (!schedule.memberId().equals(memberId.value())) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_OWNER);
        }

        // 스케줄 상태 삭제 (Hard Delete)
        scheduleCommandRepository.deleteScheduleStatusByScheduleId(scheduleId);

        // 스케줄 삭제 (Hard Delete)
        scheduleCommandRepository.deleteById(scheduleId);

        log.info("Domain: Club room usage deleted successfully");
    }

    public void createScheduleByAdmin(CreateScheduleByAdminCommand command) {
        log.info("Domain: Creating schedule by admin ID: {}", command.adminId());

        ScheduleVo scheduleVo = createScheduleVo(
                command.adminId(),
                command.title(),
                command.description(),
                command.type(),
                command.place(),
                command.startedAt(),
                command.endedAt()
        );

        ScheduleVo createdSchedule = scheduleCommandRepository.createSchedule(scheduleVo);

        // 스케줄 상태 생성 (APPROVED)
        ScheduleIdVo scheduleId = ScheduleIdVo.of(createdSchedule.id());
        ScheduleStatusVo approvedStatus = ScheduleStatusVo.approved();
        scheduleCommandRepository.createScheduleStatus(scheduleId, approvedStatus);

        log.info("Domain: Schedule created successfully by admin ID: {}", command.adminId());
    }

    public void approveOrRejectScheduleRequest(ApproveOrRejectScheduleRequestCommand command) {
        log.info("Domain: Processing schedule request ID: {}, status: {}",
                command.scheduleId(), command.status());

        ScheduleIdVo scheduleId = ScheduleIdVo.of(command.scheduleId());
        ScheduleStatusVo newStatus = ScheduleStatusVo.of(command.status());

        // 1. 스케줄 존재 확인
        validateScheduleExists(scheduleId);

        // 2. 현재 상태가 PENDING인지 확인 (비즈니스 규칙)
        validateScheduleIsPending(scheduleId);

        // 3. 상태 업데이트
        scheduleCommandRepository.updateScheduleStatus(scheduleId, newStatus);

        log.info("Domain: Schedule request processed successfully ID: {}", command.scheduleId());
    }

    public void deleteSchedule(DeleteScheduleCommand command) {
        log.info("Domain: Deleting schedule ID: {} by admin ID: {}",
                command.scheduleId(), command.adminId());

        ScheduleIdVo scheduleId = ScheduleIdVo.of(command.scheduleId());

        // 1. 존재 여부 확인
        validateScheduleExists(scheduleId);

        // 2. 스케줄 상태 삭제 (Hard Delete)
        scheduleCommandRepository.deleteScheduleStatusByScheduleId(scheduleId);

        // 3. 스케줄 삭제 (Hard Delete)
        scheduleCommandRepository.deleteById(scheduleId);

        log.info("Domain: Schedule deleted successfully ID: {}", command.scheduleId());
    }

    public List<ScheduleVo> getAllApprovedScheduleRequests(GetAllApprovedScheduleRequestsQuery query) {
        log.info("Domain: Getting all approved schedule requests for date: {}", query.date());

        ScheduleDateVo scheduleDateVo = ScheduleDateVo.of(query.date());

        return scheduleQueryRepository.findAllApprovedByMonth(scheduleDateVo);
    }

    public List<ScheduleVo> getMyRequests(GetMyRequestsQuery query) {
        log.info("Domain: Getting my requests for member ID: {}", query.memberId());

        MemberIdVo memberIdVo  = MemberIdVo.of(query.memberId());

        return scheduleQueryRepository.findAllByMemberId(memberIdVo);
    }

    public List<AdminScheduleVo> getPendingScheduleRequests(GetPendingScheduleRequestsQuery query) {
        log.info("Domain: Getting pending schedule requests for admin ID: {}", query.adminId());

        MemberIdVo memberIdVo = MemberIdVo.of(query.adminId());

        return scheduleQueryRepository.findPendingSchedulesWithMemberInfo(memberIdVo);
    }

    private ScheduleVo createScheduleVo(Long memberId, String title, String description,
                                        String type, String place,
                                        OffsetDateTime startedAt, OffsetDateTime endedAt) {
        // VO 생성 시 비즈니스 검증 자동 수행
        ScheduleTitleVo titleVo = ScheduleTitleVo.of(title);
        ScheduleDescriptionVo descriptionVo = ScheduleDescriptionVo.of(description);
        ScheduleTypeVo typeVo = ScheduleTypeVo.of(type);
        SchedulePlaceVo placeVo = SchedulePlaceVo.of(place);
        ScheduleDateTimeVo dateTimeVo = ScheduleDateTimeVo.of(startedAt, endedAt);

        return ScheduleVo.of(null, memberId, titleVo.value(), descriptionVo.value(), typeVo.value(),
                placeVo.value(), dateTimeVo.startedAt(), dateTimeVo.endedAt(),
                null, null);
    }

    private void validateScheduleExists(ScheduleIdVo scheduleId) {
        if (!scheduleQueryRepository.existsById(scheduleId)) {
            throw new DomainException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_NOT_FOUND);
        }
    }

    private void validateScheduleIsPending(ScheduleIdVo scheduleId) {
        if (!scheduleQueryRepository.isPendingStatus(scheduleId)) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_NOT_PENDING);
        }
    }
}
