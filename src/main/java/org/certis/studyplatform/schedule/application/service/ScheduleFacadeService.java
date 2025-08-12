package org.certis.studyplatform.schedule.application.service;

import jakarta.validation.Valid;
import org.certis.studyplatform.schedule.presentation.dto.request.*;
import org.certis.studyplatform.schedule.presentation.dto.response.AdminScheduleResponseDto;
import org.certis.studyplatform.schedule.presentation.dto.response.ScheduleResponseDto;

import java.time.OffsetDateTime;
import java.util.List;

public class ScheduleFacadeService {
    public void createClubRoomUsage(Long id, @Valid ClubRoomUsageRequestDto request) {
    }

    public List<ScheduleResponseDto> getMyScheduleRequests(Long id, OffsetDateTime date) {
    }

    public void deleteClubRoomUsage(Long id, @Valid ClubRoomUsageDeleteRequestDto request) {
    }


    public void createScheduleByAdmin(Long id, @Valid AdminScheduleCreateRequestDto requestDto) {
    }

    public void approveOrRejectScheduleRequest(Long id, @Valid AdminScheduleUpdateRequestDto requestDto) {
    }

    public void deleteApprovedSchedule(Long id, @Valid AdminScheduleDeleteRequestDto requestDto) {
    }

    public List<AdminScheduleResponseDto> getPendingScheduleRequests(Long id) {
    }
}
