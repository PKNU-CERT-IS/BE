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
        return new ProjectAttachedVo(id, name, type, size, attachedUrl);
    }
} 