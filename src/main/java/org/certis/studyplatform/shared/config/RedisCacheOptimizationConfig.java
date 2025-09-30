package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Redis 캐시 최적화 설정
 * - 캐시 적중률 개선
 * - 메모리 사용량 최적화
 * - 성능 모니터링
 */
@Configuration
@EnableScheduling
@Profile("prod")
@ConditionalOnProperty(name = "certis.redis.optimization.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class RedisCacheOptimizationConfig {

    @Autowired
    @Lazy
    private RedissonClient redissonClient;

    /**
     * 캐시 워밍업 및 최적화
     * 매일 오전 6시 실행 (동기화 완료 후, 트래픽이 적은 시간대)
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void optimizeCachePerformance() {
        try {
            log.info("Redis: Starting cache optimization (lightweight)");
            // GB-Hr 절감을 위해 무거운 스캔 작업 제거
        } catch (Exception e) {
            log.error("Redis: Cache optimization failed", e);
        }
    }

    /**
     * 캐시 적중률 모니터링 (무거운 스캔 제거)
     * 60분마다 실행
     */
    @Scheduled(fixedRate = 3600000)
    public void monitorCacheHitRate() {
        try {
            log.info("Redis Cache Monitoring: lightweight tick");
        } catch (Exception e) {
            log.error("Redis: Cache hit rate monitoring failed", e);
        }
    }

    /**
     * 메모리 압박 상황 대응 (총 키 수 스캔 제거)
     * 30분마다 실행
     */
    @Scheduled(fixedRate = 1800000)
    public void emergencyMemoryCleanup() {
        try {
            log.info("Redis: Emergency cleanup check (lightweight)");
        } catch (Exception e) {
            log.error("Redis: Emergency memory cleanup failed", e);
        }
    }
}
