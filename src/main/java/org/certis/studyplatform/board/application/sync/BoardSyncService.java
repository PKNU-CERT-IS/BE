package org.certis.studyplatform.board.application.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.certis.studyplatform.board.infrastructure.monitoring.BoardSyncMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardSyncService {
    private final BoardDomainService boardDomainService;
    private final BoardSyncMetrics syncMetrics;

    /**
     * Redis → RDB 통계 동기화
     * 매일 00시 배치 작업
     * 재시도 메커니즘과 상세한 에러 처리 포함
     */
    @Transactional
    public void syncStatsFromRedisToDatabase() {
        long startTime = System.currentTimeMillis();
        log.info("Application: Starting Redis to Database sync job at {}", java.time.LocalDateTime.now());

        try {
            // Domain Service에 동기화 작업 위임
            int syncedBoards = boardDomainService.syncAllBoardStats();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Application: Redis to Database sync completed successfully in {}ms", duration);

            // 메트릭 기록
            syncMetrics.recordSyncSuccess(duration, syncedBoards);

            // 성공 알림 (선택적)
            sendSuccessNotification(duration, syncedBoards);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Application: Redis to Database sync failed after {}ms", duration, e);
            
            // 메트릭 기록
            syncMetrics.recordSyncFailure(duration, e);
            
            // 실패 알림 발송
            sendFailureNotification(e, duration);
            
            // 예외 재발생으로 트랜잭션 롤백
            throw e;
        }
    }

    /**
     * 데이터 일관성 검증
     * Redis와 RDB 간 데이터 차이 확인
     */
    @Transactional(readOnly = true)
    public void validateDataConsistency() {
        log.info("Application: Starting data consistency validation");

        try {
            // Domain Service에 검증 작업 위임
            boolean isConsistent = boardDomainService.validateStatsConsistency();

            if (isConsistent) {
                log.info("Application: Data consistency validation passed");
                syncMetrics.recordConsistencySuccess();
            } else {
                log.warn("Application: Data consistency validation failed - inconsistencies found");
                syncMetrics.recordConsistencyFailure(new RuntimeException("Data inconsistencies found"));
                // TODO: 불일치 알림 발송
            }
        } catch (Exception e) {
            log.error("Application: Data consistency validation failed", e);
            syncMetrics.recordConsistencyFailure(e);
            throw e;
        }
    }

    /**
     * 동기화 성공 알림
     */
    private void sendSuccessNotification(long duration, int syncedBoards) {
        try {
            // 실제 운영에서는 Slack, Email, SMS 등으로 알림 발송
            log.info("📧 Notification: Board sync completed successfully in {}ms - Synced {} boards", duration, syncedBoards);
            
            // 현재 메트릭 상태 로깅
            var metrics = syncMetrics.getCurrentMetrics();
            log.info("📊 Current Sync Metrics: {}", metrics);
            
            // TODO: 실제 알림 시스템 연동
            // slackService.sendMessage("✅ Board stats sync completed successfully in " + duration + "ms - Synced " + syncedBoards + " boards");
            // emailService.sendEmail("admin@certis.com", "Board Sync Success", 
            //     "Sync completed in " + duration + "ms. Synced " + syncedBoards + " boards. Success rate: " + metrics.getSuccessRate() + "%");
            
        } catch (Exception e) {
            log.warn("Failed to send success notification", e);
        }
    }

    /**
     * 동기화 실패 알림
     */
    private void sendFailureNotification(Exception e, long duration) {
        try {
            // 실제 운영에서는 즉시 알림 발송
            log.error("🚨 Notification: Board sync failed after {}ms - {}", duration, e.getMessage());
            
            // TODO: 실제 알림 시스템 연동
            // slackService.sendMessage("❌ Board stats sync failed after " + duration + "ms: " + e.getMessage());
            // emailService.sendEmail("admin@certis.com", "Board Sync Failure", 
            //     "Sync failed after " + duration + "ms. Error: " + e.getMessage());
            
        } catch (Exception notificationException) {
            log.error("Failed to send failure notification", notificationException);
        }
    }
}
