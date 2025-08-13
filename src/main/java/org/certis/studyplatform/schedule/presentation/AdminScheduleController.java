package org.certis.studyplatform.schedule.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.schedule.application.service.ScheduleFacadeService;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleCreateRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleDeleteRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleUpdateRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.response.AdminScheduleResponseDto;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/schedule")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleFacadeService scheduleFacadeService;

   // 스케줄 정보 생성
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createScheduleByAdmin(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminScheduleCreateRequestDto requestDto) {

        scheduleFacadeService.createScheduleByAdmin(currentUser.getId(), requestDto);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_CREATE_SUCCESS);
    }

    // 동방 예약 승인 거절
    @PutMapping("/update")
    public ResponseEntity<GlobalResponseHandler<Void>> approveOrRejectScheduleRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminScheduleUpdateRequestDto requestDto) {

        scheduleFacadeService.approveOrRejectScheduleRequest(currentUser.getId(), requestDto);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_UPDATE_SUCCESS);
    }

   // 스케줄 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteSchedule(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminScheduleDeleteRequestDto requestDto) {

        scheduleFacadeService.deleteSchedule(currentUser.getId(),requestDto);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_DELETE_SUCCESS);
    }

    // 동방 예약 정보 조회 ( 사용자 이름 포함 )
    @GetMapping("/requests")
    public ResponseEntity<GlobalResponseHandler<List<AdminScheduleResponseDto>>> getPendingScheduleRequests(
            @AuthenticationPrincipal CurrentUser currentUser) {

        List<AdminScheduleResponseDto> schedules = scheduleFacadeService.getPendingScheduleRequests(
                currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_FIND_SUCCESS, schedules);
    }
}