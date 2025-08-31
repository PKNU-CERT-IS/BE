package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;

/**
 * 프로젝트 회의록 통계 VO
 */
public record ProjectMeetingStatisticsVo(
        String writerName,
        Integer meetingCount,
        OffsetDateTime lastMeetingDate
) {
    public static ProjectMeetingStatisticsVo of(
            String writerName,
            Integer meetingCount,
            OffsetDateTime lastMeetingDate) {
        return new ProjectMeetingStatisticsVo(writerName, meetingCount, lastMeetingDate);
    }
}
