package org.certis.studyplatform.shared.util;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * 유예기간 계산 유틸리티
 *
 * 업솔버의 벌점 유예기간 계산 로직:
 * 1. 진행 중인 활동이 있으면 → 가장 늦게 끝나는 활동의 endDate + 유예기간
 * 2. 모든 활동이 완료되었으면 → 가장 최근 완료 활동의 endDate + 유예기간
 * 3. 유예기간: 3주 이하 → 1주, 4주 이상 → 2주
 *
 * VO에 의존하지 않는 순수 유틸리티 클래스
 */
public final class GracePeriodCalculator {

    private GracePeriodCalculator() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 활동 목록을 기반으로 유예기간을 계산
     *
     * @param activities 활동 목록
     * @return 계산된 유예기간 종료 날짜 (null이면 유예기간 없음)
     */
    public static OffsetDateTime calculateGracePeriod(List<ActivityInfo> activities) {
        if (activities == null || activities.isEmpty()) {
            return null;
        }

        // 1. 진행 중인 활동이 있는지 확인
        Optional<ActivityInfo> latestOngoingActivity = findLatestOngoingActivity(activities);

        if (latestOngoingActivity.isPresent()) {
            // 진행 중인 활동이 있으면 가장 늦게 끝나는 활동 기준
            return calculateGracePeriodForActivity(
                latestOngoingActivity.get().startDate(),
                latestOngoingActivity.get().endDate()
            );
        }

        // 2. 모든 활동이 완료된 경우 가장 최근 완료 활동 기준
        Optional<ActivityInfo> mostRecentCompletedActivity = findMostRecentCompletedActivity(activities);

        if (mostRecentCompletedActivity.isPresent()) {
            return calculateGracePeriodForActivity(
                mostRecentCompletedActivity.get().startDate(),
                mostRecentCompletedActivity.get().endDate()
            );
        }

        return null; // 완료된 활동이 없으면 유예기간 없음
    }

    /**
     * 단일 활동의 유예기간을 계산
     *
     * @param startDate 활동 시작일
     * @param endDate 활동 종료일
     * @return 계산된 유예기간 종료 날짜
     */
    public static OffsetDateTime calculateGracePeriodForActivity(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        // 활동 기간 계산 (시작일부터 종료일까지)
        long activityDurationWeeks = ChronoUnit.WEEKS.between(startDate, endDate);

        // 유예기간 계산: 3주 이하 → 1주, 4주 이상 → 2주
        int gracePeriodWeeks = calculateGracePeriodWeeks(activityDurationWeeks);

        // 유예기간 = 활동 종료일 + 유예기간
        return endDate.plusWeeks(gracePeriodWeeks);
    }

    /**
     * 진행 중인 활동들 중 가장 늦게 끝나는 활동 찾기
     */
    private static Optional<ActivityInfo> findLatestOngoingActivity(List<ActivityInfo> activities) {
        return activities.stream()
                .filter(ActivityInfo::isOngoing)
                .filter(activity -> activity.endDate() != null)
                .max((a1, a2) -> a1.endDate().compareTo(a2.endDate()));
    }

    /**
     * 완료된 활동들 중 가장 최근에 끝난 활동 찾기
     */
    private static Optional<ActivityInfo> findMostRecentCompletedActivity(List<ActivityInfo> activities) {
        OffsetDateTime now = OffsetDateTime.now();

        return activities.stream()
                .filter(activity -> !activity.isOngoing())
                .filter(activity -> activity.endDate() != null)
                .filter(activity -> activity.endDate().isBefore(now) || activity.endDate().isEqual(now)) // 이미 끝난 활동만
                .max((a1, a2) -> a1.endDate().compareTo(a2.endDate()));
    }

    /**
     * 활동 기간에 따른 유예기간 계산
     *
     * @param activityDurationWeeks 활동 진행 기간 (주 단위)
     * @return 유예기간 (주 단위)
     */
    private static int calculateGracePeriodWeeks(long activityDurationWeeks) {
        if (activityDurationWeeks <= 3) {
            return 1; // 3주 이하: 1주일 유예기간
        } else {
            return 2; // 4주 이상: 2주일 유예기간
        }
    }

    /**
     * 활동 정보를 담는 레코드
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @param isOngoing 진행 중 여부
     */
    public record ActivityInfo(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        boolean isOngoing
    ) {
        /**
         * 편의 생성자 - 진행 상태 자동 판단
         */
        public static ActivityInfo of(OffsetDateTime startDate, OffsetDateTime endDate) {
            OffsetDateTime now = OffsetDateTime.now();
            boolean isOngoing = startDate != null && endDate != null &&
                               !now.isBefore(startDate) && now.isBefore(endDate);
            return new ActivityInfo(startDate, endDate, isOngoing);
        }

        /**
         * 진행 중인 활동 생성
         */
        public static ActivityInfo ongoing(OffsetDateTime startDate, OffsetDateTime endDate) {
            return new ActivityInfo(startDate, endDate, true);
        }

        /**
         * 완료된 활동 생성
         */
        public static ActivityInfo completed(OffsetDateTime startDate, OffsetDateTime endDate) {
            return new ActivityInfo(startDate, endDate, false);
        }
    }
}