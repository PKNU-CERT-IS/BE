package org.certis.studyplatform.shared.util;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 날짜/시간 관련 유틸리티 클래스
 * 
 * 스터디/프로젝트 기간 계산, 주 단위 계산 등에 사용
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 다음 주 월요일 반환
     * 
     * @param dateTime 기준 날짜시간
     * @return 다음 주 월요일 00:00:00
     */
    public static OffsetDateTime getNextMondayFrom(OffsetDateTime dateTime) {
        return dateTime.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                      .withHour(0)
                      .withMinute(0)
                      .withSecond(0)
                      .withNano(0);
    }

    /**
     * 해당 주의 일요일 오후 6시 반환
     * 
     * @param dateTime 기준 날짜시간
     * @return 해당 주 일요일 18:00:00
     */
    public static OffsetDateTime getSundayAt6PM(OffsetDateTime dateTime) {
        return dateTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                      .withHour(18)
                      .withMinute(0)
                      .withSecond(0)
                      .withNano(0);
    }

    /**
     * 현재 시간이 일요일 오후 6시 이후인지 확인
     * 
     * @return 일요일 오후 6시 이후이면 true
     */
    public static boolean isAfterSundayAt6PM() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sundayAt6PM = getSundayAt6PM(now);
        
        // 현재가 일요일이고 6시 이후인 경우
        if (now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() >= 18) {
            return true;
        }
        
        // 월요일~토요일인 경우 이전 주 일요일 6시와 비교
        if (now.getDayOfWeek() != DayOfWeek.SUNDAY) {
            OffsetDateTime lastSundayAt6PM = now.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
                                               .withHour(18)
                                               .withMinute(0)
                                               .withSecond(0)
                                               .withNano(0);
            return now.isAfter(lastSundayAt6PM);
        }
        
        return false;
    }

    /**
     * 스터디 시작 주 계산
     * 
     * 규칙:
     * - 일요일 오후 6시 이전 신청: 다음 주 월요일
     * - 일요일 오후 6시 이후 신청: 다다음 주 월요일
     * 
     * @param requestTime 신청 시간
     * @return 스터디 시작 주의 월요일
     */
    public static OffsetDateTime calculateStudyStartWeek(OffsetDateTime requestTime) {
        if (requestTime.getDayOfWeek() == DayOfWeek.SUNDAY && requestTime.getHour() < 18) {
            // 일요일 오후 6시 이전: 다음 주 월요일
            return getNextMondayFrom(requestTime);
        } else {
            // 일요일 오후 6시 이후 또는 다른 요일: 다다음 주 월요일
            OffsetDateTime nextMonday = getNextMondayFrom(requestTime);
            return nextMonday.plusWeeks(1);
        }
    }

    /**
     * 주 단위 기간 계산
     * 
     * @param startWeek 시작 주
     * @param weeks 기간 (주 단위)
     * @return 종료 주의 일요일
     */
    public static OffsetDateTime calculateEndWeek(OffsetDateTime startWeek, int weeks) {
        return startWeek.plusWeeks(weeks)
                       .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                       .withHour(23)
                       .withMinute(59)
                       .withSecond(59);
    }

    /**
     * 두 날짜 간의 주 차이 계산
     * 
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @return 주 차이
     */
    public static long getWeeksBetween(OffsetDateTime start, OffsetDateTime end) {
        return ChronoUnit.WEEKS.between(start, end);
    }

    /**
     * 현재가 특정 기간 내에 있는지 확인
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 기간 내에 있으면 true
     */
    public static boolean isWithinPeriod(OffsetDateTime startDate, OffsetDateTime endDate) {
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * D-Day 계산 (양수: 미래, 음수: 과거, 0: 오늘)
     * 
     * @param targetDate 목표 날짜
     * @return D-Day 값
     */
    public static long calculateDDay(OffsetDateTime targetDate) {
        OffsetDateTime now = OffsetDateTime.now();
        return ChronoUnit.DAYS.between(now.toLocalDate(), targetDate.toLocalDate());
    }

    /**
     * 현재 시간이 특정 날짜 이후인지 확인
     * 
     * @param dateTime 비교할 날짜시간
     * @return 이후이면 true
     */
    public static boolean isAfter(OffsetDateTime dateTime) {
        return OffsetDateTime.now().isAfter(dateTime);
    }

    /**
     * 현재 시간이 특정 날짜 이전인지 확인
     * 
     * @param dateTime 비교할 날짜시간
     * @return 이전이면 true
     */
    public static boolean isBefore(OffsetDateTime dateTime) {
        return OffsetDateTime.now().isBefore(dateTime);
    }
} 