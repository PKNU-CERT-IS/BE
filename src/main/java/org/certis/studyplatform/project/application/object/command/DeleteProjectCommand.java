package org.certis.studyplatform.project.application.object.command;

/**
 * Delete Project Command
 *
 * 프로젝트 삭제 명령 객체
 */
public record DeleteProjectCommand(
    Long id,
    Long requesterId
) {
    public static DeleteProjectCommand of(Long id, Long requesterId) {
        return new DeleteProjectCommand(id, requesterId);
    }
}