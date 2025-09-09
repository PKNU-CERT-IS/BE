package org.certis.studyplatform.blog.application.object.command;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;

/**
 * Update Blog Command
 *
 * 블로그 수정 명령 객체
 */
public record UpdateBlogCommand(
        Long id,
        String title,
        String description,
        String content,
        String category,
        ArticleReferenceType referenceType,
        Long referenceId,
        Long requesterId
) {
    public static UpdateBlogCommand of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            Long requesterId
    ) {
        return new UpdateBlogCommand(
                id, title, description, content, category,
                referenceType, referenceId, requesterId
        );
    }
}