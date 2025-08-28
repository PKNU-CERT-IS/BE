package org.certis.studyplatform.board.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardSyncScheduler {

    private final BoardSyncService boardSyncService;

    /**
     * Redis → RDB 통계 동기화
     * 매일 오전 00시 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void syncBoardStatsDaily() {
        log.info("Scheduler: Starting daily board stats sync job at 00:00");

        try {
            boardSyncService.syncStatsFromRedisToDatabase();
            log.info("Scheduler: Daily board stats sync job completed successfully");

        } catch (Exception e) {
            log.error("Scheduler: Daily board stats sync job failed", e);
        }
    }

    /**
     * 데이터 일관성 검증
     * 매일 오전 01시 실행 (동기화 1시간 후)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void validateDataConsistencyDaily() {
        log.info("Scheduler: Starting daily data consistency validation at 01:00");

        try {
            boardSyncService.validateDataConsistency();
            log.info("Scheduler: Daily data consistency validation completed");

        } catch (Exception e) {
            log.error("Scheduler: Daily data consistency validation failed", e);
        }
    }
}
