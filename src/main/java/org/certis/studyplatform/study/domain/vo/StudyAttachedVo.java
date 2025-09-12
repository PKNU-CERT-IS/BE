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
        return new StudyAttachedVo(id, name, type, size, attachedUrl);
    }
}