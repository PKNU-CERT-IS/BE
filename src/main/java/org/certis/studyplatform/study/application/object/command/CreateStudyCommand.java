package org.certis.studyplatform.study.application.object.command;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Create Study Command
 *
 * 스터디 생성 명령 객체
 */
public record CreateStudyCommand(
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
    Long creatorId
) {
public static CreateStudyCommand of(
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
        Long creatorId
    ) {
        return new CreateStudyCommand(
            title, description, content, category, subCategory,
            startDate, endDate,
            githubUrl, externalUrl, thumbnailUrl, attachedFiles,
            maxParticipants, creatorId
        );
    }
}