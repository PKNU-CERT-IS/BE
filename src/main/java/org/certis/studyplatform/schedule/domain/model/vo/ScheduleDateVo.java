package org.certis.studyplatform.schedule.domain.model.vo;

import java.time.OffsetDateTime;

public record ScheduleDateVo(OffsetDateTime value) {
    public static ScheduleDateVo of(OffsetDateTime date) {
        return new ScheduleDateVo(date);
    }
}
