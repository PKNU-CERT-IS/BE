package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;

/**
 * 스터디 회의록 통계 VO
 */
public record StudyMeetingStatisticsVo(
        String writerName,
        Integer meetingCount,
        OffsetDateTime lastMeetingDate
) {
    public static StudyMeetingStatisticsVo of(
            String writerName,
            Integer meetingCount,
            OffsetDateTime lastMeetingDate) {
        return new StudyMeetingStatisticsVo(writerName, meetingCount, lastMeetingDate);
    }
}
