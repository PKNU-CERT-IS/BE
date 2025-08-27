package org.certis.studyplatform.member.domain.vo;

import java.time.OffsetDateTime;

public record GracePeriodVo(OffsetDateTime value) {
    public static GracePeriodVo of(OffsetDateTime dateTime) {
        return new GracePeriodVo(dateTime);
    }
}