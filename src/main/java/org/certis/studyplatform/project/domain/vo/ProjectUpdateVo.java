package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Updated Value Object
 *
 * 프로젝트 수정 결과를 나타내는 불변 객체
 */
public record ProjectUpdateVo(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        List<String> skills,
        String githubUrl,
        ExternalUrlVo externalUrl,
        String demoUrl,
        Integer maxParticipants
) {
    public static ProjectUpdateVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<String> skills,
            String githubUrl,
            ExternalUrlVo externalUrl,
            String demoUrl,
            Integer maxParticipants
    ) {
        return new ProjectUpdateVo(id, title, description, content,
                category, subCategory, startDate, endDate,
                skills, githubUrl, externalUrl, demoUrl, maxParticipants);
    }
}