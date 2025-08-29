package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * 이름 Value Object
 *
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 타입 안전성과 도메인 개념 캡슐화
 */
@Embeddable
public record NameVo(String value) {

    public NameVo {
        validateName(value);
    }

    public static NameVo of(String name) {
        return new NameVo(name);
    }

    /**
     * 이름 비즈니스 규칙 검증
     */
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "이름은 필수입니다");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "이름은 2자 이상 50자 이하여야 합니다");
        }

        if (!trimmedName.matches("^[가-힣a-zA-Z\\s]+$")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "이름은 한글, 영문, 공백만 포함할 수 있습니다");
        }
    }
}