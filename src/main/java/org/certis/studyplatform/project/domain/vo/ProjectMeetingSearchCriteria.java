package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

/**
 * 프로젝트 회의록 고급 검색 조건
 */
@Getter
@Builder
public class ProjectMeetingSearchCriteria {
    private Long projectId;
    private Long writerId;
    private String keyword;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;

    public static ProjectMeetingSearchCriteria of(
            Long projectId,
            Long writerId,
            String keyword,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo) {
        return ProjectMeetingSearchCriteria.builder()
                .projectId(projectId)
                .writerId(writerId)
                .keyword(keyword)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
    }
}