package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Summary Value Object
 *
 * 프로젝트 목록 조회 시 사용되는 요약 정보
 */
public record ProjectSummaryVo(
    Long id,
    String title,
    String description,
    List<String> category,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    String projectCreatorName,
    String projectCreatorRole,
    boolean isParticipantable,
    String githubUrl,
    String externalUrl
) {
    public static ProjectSummaryVo of(
        Long id,
        String title,
        String description,
        List<String> category,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String projectCreatorName,
        String projectCreatorRole,
        boolean isParticipantable,
        String githubUrl,
        String externalUrl
    ) {
        return new ProjectSummaryVo(
            id, title, description, category,
            startDate, endDate, projectCreatorName, projectCreatorRole,
                isParticipantable, githubUrl, externalUrl
        );
    }
}