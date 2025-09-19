package org.certis.studyplatform.project.domain.vo;

/**
 * Project Attached Value Object
 *
 * 프로젝트 첨부파일 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record ProjectAttachedVo(
    Long id,
    String name,
    String type,
    String size,
    String attachedUrl
) {
    /**
     * 기본 생성자
     */
    public static ProjectAttachedVo of(
            Long id,
            String name, 
            String type,
            String size,
            String attachedUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "invalid attached name");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "invalid attached type");
        }
        if (size == null || size.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "invalid attached size");
        }
        if (attachedUrl == null || attachedUrl.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION, "invalid attached url");
        }
        return new ProjectAttachedVo(id, name, type, size, attachedUrl);
    }
} 