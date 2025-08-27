package org.certis.studyplatform.schedule.application.object.query;

public record GetMyRequestsQuery(
        Long memberId
) {
    public  static  GetMyRequestsQuery of(
            Long memberId
    )
    {
        return new GetMyRequestsQuery(memberId);
    }
}
