package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record EncodedPasswordVo (String encodedPassword) {

    public EncodedPasswordVo {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD);
        }
        if (encodedPassword.length() > 255) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD);
        }
    }

    public static EncodedPasswordVo of(String encodedPassword) {
        return new EncodedPasswordVo(encodedPassword);
    }
}
