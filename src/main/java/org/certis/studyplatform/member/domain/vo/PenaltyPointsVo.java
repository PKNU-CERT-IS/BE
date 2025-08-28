package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record PenaltyPointsVo(Integer points) {
    public static PenaltyPointsVo of(Integer points) {
        if (points <= 0) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_PENALTY,
                    "패널티 점수는 0보다 커야 합니다.");
        }
        return new PenaltyPointsVo(points);
    }
}