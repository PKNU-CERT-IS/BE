package org.certis.studyplatform.project.domain.vo;

/**
 * 월별 회의록 통계 VO
 */
public record MonthlyMeetingStatisticsVo(
        Integer month,
        Integer meetingCount,
        Integer uniqueWriters
) {
    public static MonthlyMeetingStatisticsVo of(
            Integer month,
            Integer meetingCount,
            Integer uniqueWriters) {
        return new MonthlyMeetingStatisticsVo(month, meetingCount, uniqueWriters);
    }
}
