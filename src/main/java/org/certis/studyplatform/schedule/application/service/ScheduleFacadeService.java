package org.certis.studyplatform.schedule.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.schedule.application.mapper.ScheduleApplicationMapper;
import org.certis.studyplatform.schedule.application.object.command.*;
import org.certis.studyplatform.schedule.application.object.query.GetAllApprovedScheduleRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetMyRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetPendingScheduleRequestsQuery;
import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.presentation.dto.request.*;
import org.certis.studyplatform.schedule.presentation.dto.response.AdminScheduleResponseDto;
import org.certis.studyplatform.schedule.presentation.dto.response.ScheduleResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleFacadeService {

    private final ScheduleCommandService scheduleCommandService;
    private final ScheduleQueryService scheduleQueryService;
    private final ScheduleApplicationMapper scheduleApplicationMapper;

    @Transactional
    public void createClubRoomUsage(Long memberId, ClubRoomUsageRequestDto request) {

        log.info("Facade: Creating club room usage for member ID: {}, title: {}",
                memberId, request.getTitle());

        CreateClubRoomUsageCommand command = CreateClubRoomUsageCommand.of(
                memberId,
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getStartedAt(),
                request.getEndedAt()
        );

        scheduleCommandService.createClubRoomUsage(command);

        log.info("Facade: Club room usage created successfully for member ID: {}", memberId);
    }

    public List<ScheduleResponseDto> getAllApprovedScheduleRequests(OffsetDateTime date) {
        log.info("Facade: Getting all schedule requests for date: {}", date);

        GetAllApprovedScheduleRequestsQuery query = GetAllApprovedScheduleRequestsQuery.of(date);

        List<ScheduleVo> scheduleVos = scheduleQueryService.getAllApprovedScheduleRequests(query);

        List<ScheduleResponseDto> responseDtos = scheduleApplicationMapper.toScheduleResponseDtoList(scheduleVos);

        log.info("Facade: Found {} schedule requests for date: {}", responseDtos.size(), date);
        return responseDtos;
    }

    public List<ScheduleResponseDto> getMyRequests(Long memberId) {
        log.info("Facade: Getting my requests for member ID: {}", memberId);

        GetMyRequestsQuery query = GetMyRequestsQuery.of(memberId);

        List<ScheduleVo> scheduleVos = scheduleQueryService.getMyRequests(query);

        List<ScheduleResponseDto> responseDtos = scheduleApplicationMapper.toScheduleResponseDtoList(scheduleVos);

        log.info("Facade: Found {} my requests for member ID: {}", responseDtos.size(), memberId);
        return responseDtos;
    }

    @Transactional
    public void deleteClubRoomUsage(Long memberId, ClubRoomUsageDeleteRequestDto request) {
        log.info("Facade: Deleting club room usage for member ID: {}, schedule ID: {}",
                memberId, request.getScheduleId());

        // DTO → Command Object 변환
        DeleteClubRoomUsageCommand command = DeleteClubRoomUsageCommand.of(
                memberId,
                request.getScheduleId()
        );

        // Command Service 호출
        scheduleCommandService.deleteClubRoomUsage(command);

        log.info("Facade: Club room usage deleted successfully for member ID: {}", memberId);
    }

    @Transactional
    public void createScheduleByAdmin(Long adminId, AdminScheduleCreateRequestDto request) {
        log.info("Facade: Creating schedule by admin ID: {}, title: {}",
                adminId, request.getTitle());

        CreateScheduleByAdminCommand command = CreateScheduleByAdminCommand.of(
                adminId,
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPlace(),
                request.getStartedAt(),
                request.getEndedAt()
        );

        scheduleCommandService.createScheduleByAdmin(command);

        log.info("Facade: Schedule created successfully by admin ID: {}", adminId);
    }

    @Transactional
    public void approveOrRejectScheduleRequest(Long adminId, AdminScheduleUpdateRequestDto request) {
        log.info("Facade: Processing schedule request for admin ID: {}, schedule ID: {}, status: {}",
                adminId, request.getScheduleId(), request.getStatus());

        ApproveOrRejectScheduleRequestCommand command = ApproveOrRejectScheduleRequestCommand.of(
                adminId,
                request.getScheduleId(),
                request.getStatus()
        );

        scheduleCommandService.approveOrRejectScheduleRequest(command);

        log.info("Facade: Schedule request processed successfully for admin ID: {}", adminId);
    }

    @Transactional
    public void deleteSchedule(Long adminId, AdminScheduleDeleteRequestDto request) {
        log.info("Facade: Deleting approved schedule for admin ID: {}, schedule ID: {}",
                adminId, request.getScheduleId());

        DeleteScheduleCommand command = DeleteScheduleCommand.of(
                adminId,
                request.getScheduleId()
        );

        scheduleCommandService.deleteSchedule(command);

        log.info("Facade: Approved schedule deleted successfully for admin ID: {}", adminId);
    }

    public List<AdminScheduleResponseDto> getPendingScheduleRequests(Long adminId) {
        log.info("Facade: Getting pending schedule requests for admin ID: {}", adminId);

        GetPendingScheduleRequestsQuery query = GetPendingScheduleRequestsQuery.of(adminId);

        List<AdminScheduleVo> scheduleVos = scheduleQueryService.getPendingScheduleRequests(query);

        List<AdminScheduleResponseDto> responseDtos = scheduleApplicationMapper.toAdminScheduleResponseDtoList(scheduleVos);

        log.info("Facade: Found {} pending schedule requests for admin ID: {}", responseDtos.size(), adminId);
        return responseDtos;
    }

}
