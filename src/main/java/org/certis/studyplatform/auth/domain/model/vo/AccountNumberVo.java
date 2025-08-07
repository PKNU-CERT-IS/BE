package org.certis.studyplatform.auth.domain.model.vo;

import java.util.Objects;

public record AccountNumberVo(String accountNumber) {
    public AccountNumberVo{
        Objects.requireNonNull(accountNumber, "계정번호는 필수입니다.");
        if(accountNumber.trim().isEmpty()){
            // 예외 (공백)
        }
        if(accountNumber.length()<6||accountNumber.length()>20){
            // 예외 (글자 수)
        }
    }

    public static AccountNumberVo of(String accountNumber) {
        return new AccountNumberVo(accountNumber);
    }
}
