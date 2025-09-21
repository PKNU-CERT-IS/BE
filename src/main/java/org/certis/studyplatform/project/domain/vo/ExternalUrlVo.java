package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * External URL Value Object
 *
 * Clean Architecture Domain Layer
 * 외부 URL 정보를 나타내는 불변 객체
 */
public record ExternalUrlVo(
        String title,
        String url
) {
    /**
     * Compact constructor with validation
     */
    public ExternalUrlVo {
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "외부 URL 제목은 필수입니다");
        }
        if (title.length() > 100) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "외부 URL 제목은 100자를 초과할 수 없습니다");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "외부 URL은 필수입니다");
        }
    }
}
