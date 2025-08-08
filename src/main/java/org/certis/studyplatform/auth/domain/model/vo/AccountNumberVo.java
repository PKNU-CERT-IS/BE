package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.Objects;

import static org.certis.studyplatform.exception.ExceptionStatus.AUTH_DOMAIN_INVALID_ACCOUNT_NUMBER_LENGTH;

public record AccountNumberVo(String accountNumber) {
    public AccountNumberVo{
        Objects.requireNonNull(accountNumber, "계정번호는 필수입니다.");
        if(accountNumber.length()<6||accountNumber.length()>20){
            throw new DomainException(AUTH_DOMAIN_INVALID_ACCOUNT_NUMBER_LENGTH);
        }
    }

    public static AccountNumberVo of(String accountNumber) {
        return new AccountNumberVo(accountNumber);
    }
}
