package org.certis.studyplatform.board.application.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardSyncService {
    private final BoardDomainService boardDomainService;

    /**
     * Redis → RDB 통계 동기화
     * 매일 00시 배치 작업
     */
    @Transactional
    public void syncStatsFromRedisToDatabase() {
        log.info("Application: Starting Redis to Database sync job");

        try {
            // Domain Service에 동기화 작업 위임
            boardDomainService.syncAllBoardStats();

            log.info("Application: Redis to Database sync completed successfully");

        } catch (Exception e) {
            log.error("Application: Redis to Database sync failed", e);
            // TODO: 알림 발송 (Slack, Email 등)
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
            } else {
                log.warn("Application: Data consistency validation failed - inconsistencies found");
                // TODO: 불일치 알림 발송
            }
        } catch (Exception e) {
            log.error("Application: Data consistency validation failed", e);
            throw e;
        }
    }
}
