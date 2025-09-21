package org.certis.studyplatform.blog.application.object.command;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;

/**
 * Create Blog Command
 *
 * 블로그 생성 명령 객체
 */
public record CreateBlogCommand(
        String title,
        String description,
        String content,
        String category,
        ArticleReferenceType referenceType,
        Long referenceId,
        Long creatorId,
        Boolean isPublic
) {
    public static CreateBlogCommand of(
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            Long creatorId,
            Boolean isPublic
    ) {
        return new CreateBlogCommand(
                title, description, content, category,
                referenceType, referenceId, creatorId, isPublic
        );
    }
}