package org.certis.studyplatform.member.domain.vo;

/**
 * 활동 요약 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * 회원의 최근 활동 정보를 캡슐화
 */
public record ActivitySummaryVo(
        long recentStudies,
        long recentProjects,
        long recentBlogs
) {

    /**
     * 정규화 생성자 - 음수 값 방지
     */
    public ActivitySummaryVo {
        recentStudies = Math.max(0, recentStudies);
        recentProjects = Math.max(0, recentProjects);
        recentBlogs = Math.max(0, recentBlogs);
    }

    /**
     * 빈 활동 요약 생성
     */
    public static ActivitySummaryVo empty() {
        return new ActivitySummaryVo(0, 0, 0);
    }

    /**
     * 총 활동 수 계산
     */
    public long getTotalActivities() {
        return recentStudies + recentProjects + recentBlogs;
    }

    /**
     * 활동이 있는지 확인
     */
    public boolean hasActivities() {
        return getTotalActivities() > 0;
    }

    /**
     * 활발한 사용자인지 확인 (총 활동 10개 이상)
     */
    public boolean isActiveUser() {
        return getTotalActivities() >= 10;
    }

    /**
     * 스터디 활동이 가장 많은지 확인
     */
    public boolean isStudyFocused() {
        return recentStudies > recentProjects && recentStudies > recentBlogs;
    }

    /**
     * 프로젝트 활동이 가장 많은지 확인
     */
    public boolean isProjectFocused() {
        return recentProjects > recentStudies && recentProjects > recentBlogs;
    }

    /**
     * 블로그 활동이 가장 많은지 확인
     */
    public boolean isBlogFocused() {
        return recentBlogs > recentStudies && recentBlogs > recentProjects;
    }

    /**
     * 활동 분포 균형 확인 (각 활동이 총 활동의 20% 이상)
     */
    public boolean isBalancedActivity() {
        if (!hasActivities()) return false;

        long total = getTotalActivities();
        double threshold = total * 0.2;

        return recentStudies >= threshold &&
                recentProjects >= threshold &&
                recentBlogs >= threshold;
    }
}