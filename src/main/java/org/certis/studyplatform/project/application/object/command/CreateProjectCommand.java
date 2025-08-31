package org.certis.studyplatform.project.application.object.command;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Create Project Command
 *
 * 프로젝트 생성 명령 객체
 */
public record CreateProjectCommand(
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
    List<CreateProjectAttachedCommand> attachedFiles,
    Integer maxParticipants,
    Long creatorId
) {
public static CreateProjectCommand of(
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
        List<CreateProjectAttachedCommand> attachedFiles,
        Integer maxParticipants,
        Long creatorId
    ) {
        return new CreateProjectCommand(
            title, description, content, category, subCategory,
            startDate, endDate,
            githubUrl, externalUrl, thumbnailUrl, attachedFiles,
            maxParticipants, creatorId
        );
    }
}