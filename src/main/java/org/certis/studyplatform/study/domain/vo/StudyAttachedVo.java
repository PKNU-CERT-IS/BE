package org.certis.studyplatform.study.domain.vo;

/**
 * Study Attached Value Object
 *
 * 스터디 첨부파일 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record StudyAttachedVo(
        Long id,
        String name,
        String type,
        String size,
        String attachedUrl
) {
    /**
     * 기본 생성자
     */
    public static StudyAttachedVo of(
            Long id,
            String name,
            String type,
            String size,
            String attachedUrl) {
        if (name == null || name.trim().isEmpty() || name.length() > 255) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid attached name");
        }
        if (type == null || type.trim().isEmpty() || type.length() > 10) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid attached type");
        }
        if (size == null || size.trim().isEmpty() || size.length() > 255) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid attached size");
        }
        if (attachedUrl == null || attachedUrl.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid attached url");
        }
        return new StudyAttachedVo(id, name, type, size, attachedUrl);
    }
}