package org.certis.studyplatform.study.domain.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 스터디 회의록 고급 검색 조건
 */
@Getter
@Builder
public class StudyMeetingSearchCriteria {
    private Long studyId;
    private Long writerId;
    private String keyword;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;

    public static StudyMeetingSearchCriteria of(
            Long studyId,
            Long writerId,
            String keyword,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo) {
        return StudyMeetingSearchCriteria.builder()
                .studyId(studyId)
                .writerId(writerId)
                .keyword(keyword)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
    }
}