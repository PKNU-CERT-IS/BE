package org.certis.studyplatform.blog.application.object.command;

/**
 * Delete Blog Command
 *
 * 블로그 삭제 명령 객체
 */
public record DeleteBlogCommand(
    Long id,
    Long requesterId
) {
    public static DeleteBlogCommand of(Long id, Long requesterId) {
        return new DeleteBlogCommand(id, requesterId);
    }
}