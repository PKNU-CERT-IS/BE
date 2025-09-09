package org.certis.studyplatform.blog.domain.vo;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;

/**
 * Blog Enable Reference Value Object
 *
 * 블로그 작성 시 참조 가능한 대상을 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record BlogEnableReferenceVo(
        ArticleReferenceType referenceType,
        Long referenceId,
        String title
) {
    /**
     * 기본 생성자
     */
    public static BlogEnableReferenceVo of(
            ArticleReferenceType referenceType,
            Long referenceId, String title) {
        return new BlogEnableReferenceVo(referenceType, referenceId, title);
    }
}