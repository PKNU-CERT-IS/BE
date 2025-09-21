package org.certis.studyplatform.project.application.object.command;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Update Project Command
 *
 * 프로젝트 수정 명령 객체
 */
public record UpdateProjectCommand(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String githubUrl,
        org.certis.studyplatform.project.domain.vo.ExternalUrlVo externalUrl,
        String demoUrl,
        String thumbnailUrl,
        List<CreateProjectAttachedCommand> attachedFiles,
        Integer maxParticipants,
        Long requesterId  // creatorId → requesterId로 변경
) {
    public static UpdateProjectCommand of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String githubUrl,
            org.certis.studyplatform.project.domain.vo.ExternalUrlVo externalUrl,
            String demoUrl,
            String thumbnailUrl,
            List<CreateProjectAttachedCommand> attachedFiles,
            Integer maxParticipants,
            Long requesterId  // creatorId → requesterId로 변경
    ) {
        return new UpdateProjectCommand(
                id, title, description, content, category, subCategory,
                startDate, endDate, githubUrl, externalUrl, demoUrl, thumbnailUrl,
                attachedFiles, maxParticipants, requesterId  // 매개변수명도 수정
        );
    }
}