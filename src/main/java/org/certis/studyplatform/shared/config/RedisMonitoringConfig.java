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
 * Redis 모니터링 및 성능 최적화 설정
 * - ElastiCache 사용량 모니터링
 * - 연결 상태 확인
 * - 성능 메트릭 수집
 */
@Configuration
@EnableScheduling
@Profile("prod")
@ConditionalOnProperty(name = "certis.redis.monitoring.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class RedisMonitoringConfig {

    @Autowired
    @Lazy
    private RedissonClient redissonClient;

    /**
     * Redis 연결 상태 모니터링
     * 15분마다 실행 (가벼운 호출)
     */
    @Scheduled(fixedRate = 900000)
    public void monitorRedisConnection() {
        try {
            long startTime = System.currentTimeMillis();
            // 가벼운 연결 확인: 짧은 TTL로 버킷에 값 설정
            redissonClient.getBucket("certis:monitor:health").set("ok");
            long responseTime = System.currentTimeMillis() - startTime;

            log.debug("Redis: Connection healthy, response time: {}ms", responseTime);

            if (responseTime > 1000) {
                log.warn("Redis: Slow response detected: {}ms", responseTime);
            }

        } catch (Exception e) {
            log.error("Redis: Connection issues detected", e);
        }
    }

    /**
     * Redis 메모리 사용량 모니터링
     * 60분마다 실행 (무거운 스캔 제거)
     */
    @Scheduled(fixedRate = 3600000)
    public void monitorRedisMemoryUsage() {
        try {
            // 무거운 패턴 스캔/카운트를 피함: 간단한 로그만 남김
            log.info("Redis: Lightweight monitoring tick");
        } catch (Exception e) {
            log.error("Redis: Memory monitoring failed", e);
        }
    }

    /**
     * Redis 키 만료 정리 작업 (TTL 기반)
     * 12시간마다 실행 (강제 스캔 제거)
     */
    @Scheduled(fixedRate = 43200000)
    public void cleanupExpiredKeys() {
        try {
            log.info("Redis: Skipped expired keys scan (TTL-based cleanup only)");
        } catch (Exception e) {
            log.error("Redis: Expired keys cleanup failed", e);
        }
    }

    /**
     * Redis 성능 메트릭 수집
     * 4시간마다 실행 (무거운 메트릭 제거)
     */
    @Scheduled(fixedRate = 14400000)
    public void collectPerformanceMetrics() {
        try {
            // 무거운 메트릭 수집 제거
            log.info("Redis Performance: lightweight metrics collection tick");
        } catch (Exception e) {
            log.error("Redis: Performance metrics collection failed", e);
        }
    }
}
