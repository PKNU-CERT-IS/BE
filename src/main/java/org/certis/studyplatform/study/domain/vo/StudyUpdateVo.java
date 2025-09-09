package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Updated Value Object
 *
 * 스터디 수정 결과를 나타내는 불변 객체
 */
public record StudyUpdateVo(
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
        String externalUrl,
        Integer maxParticipants
) {
    public static StudyUpdateVo of(
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
            String externalUrl,
            Integer maxParticipants
    ) {
        return new StudyUpdateVo(id, title, description, content,
                category, subCategory, startDate, endDate,
                skills, githubUrl, externalUrl, maxParticipants);
    }
}