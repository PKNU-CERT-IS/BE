package org.certis.studyplatform.schedule.application.object.query;

import java.time.OffsetDateTime;

public record GetAllApprovedScheduleRequestsQuery(
        OffsetDateTime date
) {
    public static GetAllApprovedScheduleRequestsQuery of(
            OffsetDateTime date
    ){
        return new GetAllApprovedScheduleRequestsQuery(date);
    }
}
