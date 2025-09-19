package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;



public record AccountNumberVo(String accountNumber) {
    public AccountNumberVo{
        if(accountNumber == null || accountNumber.trim().isEmpty()){
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_INVALID_ACCOUNT_NUMBER_LENGTH);
        }
        if(accountNumber.length()>20){
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_INVALID_ACCOUNT_NUMBER_LENGTH);
        }
    }

    public static AccountNumberVo of(String accountNumber) {
        return new AccountNumberVo(accountNumber);
    }
}
