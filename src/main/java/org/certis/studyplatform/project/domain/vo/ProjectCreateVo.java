package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;

/**
 * Project Created Value Object
 *
 * 프로젝트 생성 결과를 나타내는 불변 객체
 */
public record ProjectCreateVo(
    Long id,
    String title,
    String leaderName,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    OffsetDateTime createdAt
) {
    public static ProjectCreateVo of(
        Long id,
        String title,
        String leaderName,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime createdAt
    ) {
        return new ProjectCreateVo(id, title, leaderName, startDate, endDate, createdAt);
    }
}