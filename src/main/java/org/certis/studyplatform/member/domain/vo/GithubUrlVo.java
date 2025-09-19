package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record GithubUrlVo(String value) {

    public GithubUrlVo {
        if (value != null && !value.trim().isEmpty() && value.trim().length() > 255) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_RULE_VIOLATION, "github url must be <= 255 chars");
        }
    }

    public static GithubUrlVo of(String value) {
        return new GithubUrlVo(value);
    }
}


