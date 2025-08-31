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

    /**
     * Semester 문자열을 파싱하여 시작일과 종료일을 반환
     * 
     * 형식: "YYYY-01" (1학기) 또는 "YYYY-02" (2학기)
     * - 01: 해당 연도 3월 ~ 8월 (1학기)
     * - 02: 해당 연도 9월 ~ 다음 연도 2월 (2학기)
     *
     * @param semester 학기 문자열 (예: "2025-01", "2024-02")
     * @return 학기의 시작일과 종료일을 담은 SemesterPeriod
     * @throws IllegalArgumentException 잘못된 형식의 semester 문자열인 경우
     */
    public static SemesterPeriod parseSemesterPeriod(String semester) {
        if (semester == null || semester.trim().isEmpty()) {
            throw new IllegalArgumentException("Semester는 필수입니다");
        }

        String[] parts = semester.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Semester 형식이 올바르지 않습니다. 예: 2025-01, 2024-02");
        }

        try {
            int year = Integer.parseInt(parts[0]);
            String semesterType = parts[1];

            OffsetDateTime startDate;
            OffsetDateTime endDate;

            switch (semesterType) {
                case "01": // 1학기: 3월 ~ 8월
                    startDate = OffsetDateTime.now()
                            .withYear(year)
                            .withMonth(3)
                            .withDayOfMonth(1)
                            .withHour(0)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    endDate = OffsetDateTime.now()
                            .withYear(year)
                            .withMonth(8)
                            .withDayOfMonth(31)
                            .withHour(23)
                            .withMinute(59)
                            .withSecond(59)
                            .withNano(999999999);
                    break;
                case "02": // 2학기: 9월 ~ 다음 년도 2월
                    startDate = OffsetDateTime.now()
                            .withYear(year)
                            .withMonth(9)
                            .withDayOfMonth(1)
                            .withHour(0)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    endDate = OffsetDateTime.now()
                            .withYear(year + 1)
                            .withMonth(2)
                            .withDayOfMonth(28) // 2월의 마지막 날 (윤년은 고려하지 않음)
                            .withHour(23)
                            .withMinute(59)
                            .withSecond(59)
                            .withNano(999999999);
                    // 윤년 처리
                    if (isLeapYear(year + 1)) {
                        endDate = endDate.withDayOfMonth(29);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Semester 타입은 01(1학기) 또는 02(2학기)만 지원됩니다");
            }

            return new SemesterPeriod(startDate, endDate, year, semesterType);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("연도는 숫자여야 합니다: " + parts[0]);
        }
    }

    /**
     * 윤년 여부 확인
     * 
     * @param year 연도
     * @return 윤년이면 true
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Semester 기간 정보를 담는 Record
     */
    public record SemesterPeriod(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        int year,
        String semesterType
    ) {
        /**
         * 특정 날짜가 이 학기 기간에 포함되는지 확인
         */
        public boolean contains(OffsetDateTime dateTime) {
            return !dateTime.isBefore(startDate) && !dateTime.isAfter(endDate);
        }

        /**
         * 학기 설명 반환
         */
        public String getDescription() {
            return year + "년 " + (semesterType.equals("01") ? "1학기" : "2학기");
        }
    }
}