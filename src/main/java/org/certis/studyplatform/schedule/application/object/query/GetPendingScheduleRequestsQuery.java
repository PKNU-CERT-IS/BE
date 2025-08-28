package org.certis.studyplatform.schedule.application.object.query;

public record GetPendingScheduleRequestsQuery(
        Long adminId
) {
    public static GetPendingScheduleRequestsQuery of(
            Long adminId
    ) {
        return new GetPendingScheduleRequestsQuery(adminId);
    }
}
