package org.certis.studyplatform.member.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.GracePeriodService;
// import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 벌점 자동 부여 스케줄러
 * 
 * 매주 일요일 24:00에 유예기간이 만료된 Upsolver들에게 벌점을 부여
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PenaltyScheduler {

    private final GracePeriodService gracePeriodService;

    /**
     * 매주 일요일 자정에 유예기간 만료 벌점 처리
     * 
     * 크론 표현식: "0 0 0 * * SUN" = 매주 일요일 00:00:00
     */
    // @Scheduled(cron = "0 0 0 * * SUN", zone = "Asia/Seoul")
    public void processExpiredGracePeriods() {
        log.info("Scheduler: Starting weekly penalty processing for expired grace periods");
        
        try {
            gracePeriodService.applyExpiredGracePeriods();
            log.info("Scheduler: Weekly penalty processing completed successfully");
        } catch (Exception e) {
            log.error("Scheduler: Failed to process expired grace periods", e);
            // 스케줄러 실패가 시스템을 중단시키지 않도록 예외를 잡음
        }
    }

    /**
     * 테스트용 수동 벌점 처리 (운영 환경에서는 비활성화 권장)
     * 
     * 매분 실행 (테스트 목적)
     */
    // @Scheduled(cron = "0 * * * * *") // 매분 실행 - 테스트용
    public void processExpiredGracePeriods_ForTesting() {
        log.debug("Scheduler: Manual penalty processing for testing");
        
        try {
            gracePeriodService.applyExpiredGracePeriods();
            log.debug("Scheduler: Manual penalty processing completed");
        } catch (Exception e) {
            log.error("Scheduler: Failed to process expired grace periods in test mode", e);
        }
    }

    /**
     * 시스템 상태 체크 (매일 오전 9시)
     * 
     * 벌점 시스템의 상태를 확인하고 로그를 남김
     */
    // @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void checkPenaltySystemHealth() {
        log.info("Scheduler: Daily penalty system health check");
        
        try {
            // 여기에 시스템 상태 체크 로직 추가 가능
            // 예: 유예기간 만료 예정자 수 확인, 벌점 5점 이상 회원 수 확인 등
            
            log.info("Scheduler: Penalty system health check completed");
        } catch (Exception e) {
            log.error("Scheduler: Penalty system health check failed", e);
        }
    }
}
