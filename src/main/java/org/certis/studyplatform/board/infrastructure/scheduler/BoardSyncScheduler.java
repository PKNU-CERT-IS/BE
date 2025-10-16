package org.certis.studyplatform.board.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardSyncScheduler {

    private final BoardSyncService boardSyncService;

    /**
     * Redis → RDB 통계 동기화
     * 매일 오전 00시 실행
     * 재시도 메커니즘과 상세한 모니터링 포함
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void syncBoardStatsDaily() {
        long startTime = System.currentTimeMillis();
        log.info("🔄 Scheduler: Starting daily board stats sync job at {}", LocalDateTime.now());

        try {
            // 동기화 실행
            boardSyncService.syncStatsFromRedisToDatabase();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Scheduler: Daily board stats sync job completed successfully in {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Scheduler: Daily board stats sync job failed after {}ms", duration, e);
            
            // 스케줄러 레벨에서 추가 알림
            sendSchedulerFailureNotification(e, duration);
            
            // 예외 재발생으로 재시도 트리거
            throw e;
        }
    }

    /**
     * 데이터 일관성 검증
     * 매일 오전 01시 실행 (동기화 1시간 후)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void validateDataConsistencyDaily() {
        long startTime = System.currentTimeMillis();
        log.info("🔍 Scheduler: Starting daily data consistency validation at {}", LocalDateTime.now());

        try {
            boardSyncService.validateDataConsistency();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Scheduler: Daily data consistency validation completed in {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Scheduler: Daily data consistency validation failed after {}ms", duration, e);
            
            // 일관성 검증 실패 알림
            sendConsistencyFailureNotification(e, duration);
        }
    }

    /**
     * 스케줄러 실패 알림
     */
    private void sendSchedulerFailureNotification(Exception e, long duration) {
        try {
            log.error("🚨 Scheduler Notification: Board sync scheduler failed after {}ms - {}", duration, e.getMessage());
            
            // TODO: 실제 알림 시스템 연동
            // slackService.sendMessage("🚨 Board sync scheduler failed after " + duration + "ms: " + e.getMessage());
            
        } catch (Exception notificationException) {
            log.error("Failed to send scheduler failure notification", notificationException);
        }
    }

    /**
     * 일관성 검증 실패 알림
     */
    private void sendConsistencyFailureNotification(Exception e, long duration) {
        try {
            log.error("🚨 Consistency Notification: Data consistency validation failed after {}ms - {}", duration, e.getMessage());
            
            // TODO: 실제 알림 시스템 연동
            // slackService.sendMessage("🚨 Data consistency validation failed after " + duration + "ms: " + e.getMessage());
            
        } catch (Exception notificationException) {
            log.error("Failed to send consistency failure notification", notificationException);
        }
    }
}
