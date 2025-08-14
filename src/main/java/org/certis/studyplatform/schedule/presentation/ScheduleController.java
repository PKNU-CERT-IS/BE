package org.certis.studyplatform.schedule.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.schedule.application.service.ScheduleFacadeService;
import org.certis.studyplatform.schedule.presentation.dto.request.ClubRoomUsageDeleteRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.ClubRoomUsageRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.response.ScheduleResponseDto;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.shared.security.MockCurrentUserProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleFacadeService scheduleFacadeService;
    private final MockCurrentUserProvider mockCurrentUserProvider;

   // 동방 사용 요청 ( status 가 PENDING 상태이며 장소는 동아리방인 스케줄을 생성함 )
    @PostMapping("/request")
    public ResponseEntity<GlobalResponseHandler<Void>> createClubRoomUsage(
//            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ClubRoomUsageRequestDto request) {

        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        scheduleFacadeService.createClubRoomUsage(currentUser.getId(), request);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_CREATE_SUCCESS);
    }


    //  스케줄 조회 (월별)
    @GetMapping("/requests")
    public ResponseEntity<GlobalResponseHandler<List<ScheduleResponseDto>>> getAllApprovedScheduleRequests(
            @RequestParam OffsetDateTime date) {

        List<ScheduleResponseDto> schedules = scheduleFacadeService.getAllApprovedScheduleRequests(date);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_FIND_SUCCESS, schedules);
    }

    // 스케줄 조회 ( 회원 자신의 것 )
    @GetMapping("/me/request")
    public ResponseEntity<GlobalResponseHandler<List<ScheduleResponseDto>>> getMyRequests(
//            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        List<ScheduleResponseDto> schedules = scheduleFacadeService.getMyRequests(
                currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_FIND_SUCCESS, schedules);
    }

    // 동방 예약 정보 삭제
    @DeleteMapping("/request/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteClubRoomUsage(
//            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ClubRoomUsageDeleteRequestDto request) {

        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        scheduleFacadeService.deleteClubRoomUsage(currentUser.getId(), request);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_DELETE_SUCCESS);
    }
}