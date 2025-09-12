package org.certis.studyplatform.study.application.object.command;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Update Study Command
 *
 * 스터디 수정 명령 객체
 */
public record UpdateStudyCommand(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String githubUrl,
        String externalUrl,
        String thumbnailUrl,
        List<CreateStudyAttachedCommand> attachedFiles,
        Integer maxParticipants,
        Long requesterId  // creatorId → requesterId로 변경
) {
    public static UpdateStudyCommand of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String githubUrl,
            String externalUrl,
            String thumbnailUrl,
            List<CreateStudyAttachedCommand> attachedFiles,
            Integer maxParticipants,
            Long requesterId  // creatorId → requesterId로 변경
    ) {
        return new UpdateStudyCommand(
                id, title, description, content, category, subCategory,
                startDate, endDate, githubUrl, externalUrl, thumbnailUrl,
                attachedFiles, maxParticipants, requesterId  // 매개변수명도 수정
        );
    }
}