package org.certis.studyplatform.study.application.object.command;

/**
 * Delete Study Command
 *
 * 스터디 삭제 명령 객체
 */
public record DeleteStudyCommand(
    Long id,
    Long requesterId
) {
    public static DeleteStudyCommand of(Long id, Long requesterId) {
        return new DeleteStudyCommand(id, requesterId);
    }
}