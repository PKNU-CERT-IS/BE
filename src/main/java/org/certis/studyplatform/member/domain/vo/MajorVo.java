package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * 전공 Value Object
 *
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 전공명 형식과 규칙을 도메인에서 관리
 */
@Embeddable
public record MajorVo(String value) {

    public MajorVo {
        validateMajor(value);
    }

    public static MajorVo of(String major) {
        return new MajorVo(major);
    }

    /**
     * 전공 비즈니스 규칙 검증
     */
    private static void validateMajor(String major) {
        if (major == null || major.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_MAJOR,
                    "전공은 필수입니다");
        }

        String trimmedMajor = major.trim();

        if (!trimmedMajor.matches("^[가-힣a-zA-Z0-9 \\-(){}<>,.·&/+:;_|]+$")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_MAJOR,
                    "전공은 허용되지 않는 문자를 포함할 수 없습니다");
        }
    }
}