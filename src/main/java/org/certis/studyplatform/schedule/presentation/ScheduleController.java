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

   // 동방 사용 요청
    @PostMapping("/request")
    public ResponseEntity<GlobalResponseHandler<Void>> createClubRoomUsage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ClubRoomUsageRequestDto request) {

        scheduleFacadeService.createClubRoomUsage(currentUser.getId(), request);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_CREATE_SUCCESS);
    }


    //  내 동방 예약 요청 조회 (월별)
    @GetMapping("/requests")
    public ResponseEntity<GlobalResponseHandler<List<ScheduleResponseDto>>> getMyScheduleRequests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime date) {

        List<ScheduleResponseDto> schedules = scheduleFacadeService.getMyScheduleRequests(
                currentUser.getId(), date);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_FIND_SUCCESS, schedules);
    }

    // 동방 예약 정보 삭제
    @DeleteMapping("/request/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteClubRoomUsage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ClubRoomUsageDeleteRequestDto request) {

        scheduleFacadeService.deleteClubRoomUsage(currentUser.getId(), request);

        return GlobalResponseHandler.success(ResponseStatus.SCHEDULE_DELETE_SUCCESS);
    }
}