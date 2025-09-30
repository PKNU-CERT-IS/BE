package org.certis.studyplatform.board.infrastructure.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Board 동기화 메트릭 수집 및 모니터링
 * 동기화 성능과 상태를 추적합니다.
 */
@Component
@Slf4j
public class BoardSyncMetrics {

    // 동기화 성공/실패 카운터
    private final AtomicLong syncSuccessCount = new AtomicLong(0);
    private final AtomicLong syncFailureCount = new AtomicLong(0);
    
    // 마지막 동기화 시간
    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();
    
    // 마지막 동기화 소요 시간 (ms)
    private final AtomicLong lastSyncDuration = new AtomicLong(0);
    
    // 동기화된 총 게시글 수
    private final AtomicLong totalSyncedBoards = new AtomicLong(0);
    
    // 일관성 검증 성공/실패 카운터
    private final AtomicLong consistencySuccessCount = new AtomicLong(0);
    private final AtomicLong consistencyFailureCount = new AtomicLong(0);

    /**
     * 동기화 성공 기록
     */
    public void recordSyncSuccess(long duration, int syncedBoards) {
        syncSuccessCount.incrementAndGet();
        lastSyncTime.set(LocalDateTime.now());
        lastSyncDuration.set(duration);
        totalSyncedBoards.addAndGet(syncedBoards);
        
        log.info("📊 Metrics: Sync success recorded - Duration: {}ms, Synced boards: {}, Total success: {}", 
                duration, syncedBoards, syncSuccessCount.get());
    }

    /**
     * 동기화 실패 기록
     */
    public void recordSyncFailure(long duration, Exception e) {
        syncFailureCount.incrementAndGet();
        lastSyncTime.set(LocalDateTime.now());
        lastSyncDuration.set(duration);
        
        log.error("📊 Metrics: Sync failure recorded - Duration: {}ms, Error: {}, Total failures: {}", 
                duration, e.getMessage(), syncFailureCount.get());
    }

    /**
     * 일관성 검증 성공 기록
     */
    public void recordConsistencySuccess() {
        consistencySuccessCount.incrementAndGet();
        log.debug("📊 Metrics: Consistency validation success recorded - Total: {}", consistencySuccessCount.get());
    }

    /**
     * 일관성 검증 실패 기록
     */
    public void recordConsistencyFailure(Exception e) {
        consistencyFailureCount.incrementAndGet();
        log.warn("📊 Metrics: Consistency validation failure recorded - Error: {}, Total failures: {}", 
                e.getMessage(), consistencyFailureCount.get());
    }

    /**
     * 현재 메트릭 상태 조회
     */
    public SyncMetricsSnapshot getCurrentMetrics() {
        return new SyncMetricsSnapshot(
                syncSuccessCount.get(),
                syncFailureCount.get(),
                lastSyncTime.get(),
                lastSyncDuration.get(),
                totalSyncedBoards.get(),
                consistencySuccessCount.get(),
                consistencyFailureCount.get()
        );
    }

    /**
     * 메트릭 스냅샷 클래스
     */
    public record SyncMetricsSnapshot(
            long syncSuccessCount,
            long syncFailureCount,
            LocalDateTime lastSyncTime,
            long lastSyncDuration,
            long totalSyncedBoards,
            long consistencySuccessCount,
            long consistencyFailureCount
    ) {
        public double getSuccessRate() {
            long total = syncSuccessCount + syncFailureCount;
            return total > 0 ? (double) syncSuccessCount / total * 100 : 0.0;
        }

        public double getConsistencySuccessRate() {
            long total = consistencySuccessCount + consistencyFailureCount;
            return total > 0 ? (double) consistencySuccessCount / total * 100 : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "SyncMetrics{success: %d, failure: %d, successRate: %.2f%%, " +
                    "lastSync: %s, duration: %dms, totalBoards: %d, " +
                    "consistency: %.2f%%}",
                    syncSuccessCount, syncFailureCount, getSuccessRate(),
                    lastSyncTime, lastSyncDuration, totalSyncedBoards,
                    getConsistencySuccessRate()
            );
        }
    }
}
