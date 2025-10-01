package org.certis.studyplatform.blog.domain.vo;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;

import java.time.OffsetDateTime;

/**
 * Blog Summary Value Object
 *
 * 블로그 목록 조회 시 사용되는 요약 정보
 */
public record BlogSummaryVo(
        BlogIdVo id,
        String title,
        String description,
        String category,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String blogCreatorName,
        String blogCreatorProfileImageUrl,
        ArticleReferenceType referenceType,
        String referenceTitle,
        Integer views,
        Long studyId,
        Long projectId,
        String studyTitle,
        String projectTitle
) {
    public static BlogSummaryVo of(
            BlogIdVo id,
            String title,
            String description,
            String category,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String blogCreatorName,
            String blogCreatorProfileImageUrl,
            ArticleReferenceType referenceType,
            String referenceTitle,
            Integer views,
            Long studyId,
            Long projectId,
            String studyTitle,
            String projectTitle
    ) {
        return new BlogSummaryVo(
                id, title, description, category,
                createdAt, updatedAt, blogCreatorName, blogCreatorProfileImageUrl,
                referenceType, referenceTitle, views,
                studyId, projectId, studyTitle, projectTitle
        );
    }
}