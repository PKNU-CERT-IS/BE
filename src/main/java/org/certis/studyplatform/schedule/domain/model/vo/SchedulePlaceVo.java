package org.certis.studyplatform.schedule.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record SchedulePlaceVo(String value) {

    public static SchedulePlaceVo of(String place) {
        return new SchedulePlaceVo(place);
    }

    // 동방 빌리는 로직의 vo에 쓸 정적 팩토리 메소드
    public static SchedulePlaceVo clubroom() {
        return new SchedulePlaceVo("동아리방");
    }

    public SchedulePlaceVo {
        validatePlace(value);
    }

    private void validatePlace(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_PLACE);
        }
        if (value.trim().length() > 30) {
            throw new DomainException(ExceptionStatus.SCHEDULE_DOMAIN_INVALID_PLACE);
        }
    }
}

