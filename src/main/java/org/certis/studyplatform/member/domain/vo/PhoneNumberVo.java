package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.regex.Pattern;

public record PhoneNumberVo(String value) {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$"); // 한국 휴대폰 기본 패턴 (예시)

    public PhoneNumberVo {
        if (value == null || value.isBlank()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PHONE_NUMBER,"전화번호는 필수 입력값입니다.");
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PHONE_NUMBER,"전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)");
        }
    }

    public static PhoneNumberVo of(String value) {
        return new PhoneNumberVo(value);
    }
}
