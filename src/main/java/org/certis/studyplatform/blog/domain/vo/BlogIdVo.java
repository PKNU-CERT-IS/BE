package org.certis.studyplatform.blog.domain.vo;

/**
 * Blog ID Value Object
 *
 * 블로그 ID를 감싸는 도메인 VO
 * Type Safety 보장 및 도메인 규칙 캡슐화
 */
public record BlogIdVo(Long value) {

    public static BlogIdVo of(Long id) {
//        if (id == null || id <= 0) {
//            throw new IllegalArgumentException("Blog ID must be positive");
//        }
        return new BlogIdVo(id);
    }
}