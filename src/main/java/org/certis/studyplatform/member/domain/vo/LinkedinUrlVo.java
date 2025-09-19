package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record LinkedinUrlVo(String value) {

    public LinkedinUrlVo {
        if (value != null && !value.trim().isEmpty() && value.trim().length() > 255) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_RULE_VIOLATION, "linkedin url must be <= 255 chars");
        }
    }

    public static LinkedinUrlVo of(String value) {
        return new LinkedinUrlVo(value);
    }
}


