package org.certis.studyplatform.schedule.domain.service;

import org.certis.studyplatform.schedule.application.object.command.*;
import org.certis.studyplatform.schedule.application.object.query.GetAllApprovedScheduleRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetMyRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetPendingScheduleRequestsQuery;
import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;

import java.util.List;

public class ScheduleDomainService {
    public List<ScheduleVo> getAllApprovedScheduleRequests(GetAllApprovedScheduleRequestsQuery query) {
    }

    public List<ScheduleVo> getMyRequests(GetMyRequestsQuery query) {
    }

    public List<AdminScheduleVo> getPendingScheduleRequests(GetPendingScheduleRequestsQuery query) {
    }

    public void createClubRoomUsage(CreateClubRoomUsageCommand command) {
    }

    public void validateDeletePermission(DeleteClubRoomUsageCommand command) {
    }

    public void deleteClubRoomUsage(DeleteClubRoomUsageCommand command) {
    }

    public void createScheduleByAdmin(CreateScheduleByAdminCommand command) {
    }

    public void approveOrRejectScheduleRequest(ApproveOrRejectScheduleRequestCommand command) {
    }

    public void deleteSchedule(DeleteScheduleCommand command) {
    }
}
