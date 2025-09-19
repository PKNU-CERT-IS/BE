package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record ScheduleTypeVo(String value) {

    private static final String MEETING = "MEETING";
    private static final String WORKSHOP = "WORKSHOP";
    private static final String STUDY = "STUDY";
    private static final String CONFERENCE = "CONFERENCE";

    public static ScheduleTypeVo of(String type) {
        return new ScheduleTypeVo(type);
    }

    public ScheduleTypeVo {
        validateType(value);
    }

    private void validateType(String value) {
        if (!MEETING.equals(value) && !WORKSHOP.equals(value) && !STUDY.equals(value) && !CONFERENCE.equals(value)) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_TYPE);
        }
    }
}