package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Redis 모니터링 및 성능 최적화 설정
 * - ElastiCache 사용량 모니터링
 * - 연결 상태 확인
 * - 성능 메트릭 수집
 */
@Configuration
@EnableScheduling
@Profile("prod")
@Slf4j
public class RedisMonitoringConfig {

    @Autowired
    @Lazy
    private RedissonClient redissonClient;

    /**
     * Redis 연결 상태 모니터링
     * 2분마다 실행 (더 빈번한 모니터링)
     */
    @Scheduled(fixedRate = 120000) // 2분 = 120,000ms
    public void monitorRedisConnection() {
        try {
            long startTime = System.currentTimeMillis();
            // Redis 연결 상태 확인 (간단한 ping 테스트)
            long keyCount = redissonClient.getKeys().count();
            long responseTime = System.currentTimeMillis() - startTime;
            
            log.debug("Redis: Connection healthy, key count: {}, response time: {}ms", keyCount, responseTime);
            
            // 응답 시간이 1초를 초과하면 경고
            if (responseTime > 1000) {
                log.warn("Redis: Slow response detected: {}ms", responseTime);
            }
            
        } catch (Exception e) {
            log.error("Redis: Connection issues detected", e);
        }
    }

    /**
     * Redis 메모리 사용량 모니터링
     * 5분마다 실행 (더 빈번한 모니터링)
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void monitorRedisMemoryUsage() {
        try {
            // Redis 키 개수로 간접적인 사용량 모니터링
            long keyCount = redissonClient.getKeys().count();
            
            // 패턴별 키 개수 모니터링
            long blogKeys = redissonClient.getKeys().getKeysByPattern("certis:blog:*").spliterator().getExactSizeIfKnown();
            long boardKeys = redissonClient.getKeys().getKeysByPattern("certis:board:*").spliterator().getExactSizeIfKnown();
            
            log.info("Redis: Total keys: {}, Blog keys: {}, Board keys: {}", keyCount, blogKeys, boardKeys);
            
            // 임계값 기반 경고
            if (keyCount > 5000) {
                log.warn("Redis: High key count detected: {} (Blog: {}, Board: {})", keyCount, blogKeys, boardKeys);
            }
            
            if (blogKeys > 2000) {
                log.warn("Redis: High blog key count detected: {}", blogKeys);
            }
            
            if (boardKeys > 2000) {
                log.warn("Redis: High board key count detected: {}", boardKeys);
            }
            
        } catch (Exception e) {
            log.error("Redis: Memory monitoring failed", e);
        }
    }

    /**
     * Redis 키 만료 정리 작업 (메모리 효율성 개선)
     * 매 6시간마다 실행 (더 빈번한 정리)
     */
    @Scheduled(fixedRate = 21600000) // 6시간 = 21,600,000ms
    public void cleanupExpiredKeys() {
        try {
            log.info("Redis: Starting selective expired keys cleanup");
            
            int cleanedCount = 0;
            int totalChecked = 0;
            
            // Board 키들 정리
            for (String key : redissonClient.getKeys().getKeysByPattern("certis:board:*")) {
                totalChecked++;
                long ttl = redissonClient.getKeys().remainTimeToLive(key);
                if (ttl <= 0) {
                    redissonClient.getKeys().delete(key);
                    cleanedCount++;
                }
            }
            
            // Blog 키들 정리
            for (String key : redissonClient.getKeys().getKeysByPattern("certis:blog:*")) {
                totalChecked++;
                long ttl = redissonClient.getKeys().remainTimeToLive(key);
                if (ttl <= 0) {
                    redissonClient.getKeys().delete(key);
                    cleanedCount++;
                }
            }
            
            log.info("Redis: Cleaned {} expired keys out of {} checked keys", cleanedCount, totalChecked);
            
            // 정리 후 메모리 사용량 확인
            long remainingKeys = redissonClient.getKeys().count();
            log.info("Redis: Remaining keys after cleanup: {}", remainingKeys);
            
        } catch (Exception e) {
            log.error("Redis: Expired keys cleanup failed", e);
        }
    }
    
    /**
     * Redis 성능 메트릭 수집
     * 매시간 실행
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3,600,000ms
    public void collectPerformanceMetrics() {
        try {
            long startTime = System.currentTimeMillis();
            
            // 다양한 작업의 응답 시간 측정
            long keyCount = redissonClient.getKeys().count();
            long countTime = System.currentTimeMillis() - startTime;
            
            startTime = System.currentTimeMillis();
            long blogKeys = redissonClient.getKeys().getKeysByPattern("certis:blog:*").spliterator().getExactSizeIfKnown();
            long patternTime = System.currentTimeMillis() - startTime;
            
            log.info("Redis Performance: Key count: {} ({}ms), Pattern search: {} ({}ms)", 
                    keyCount, countTime, blogKeys, patternTime);
            
            // 성능 임계값 체크
            if (countTime > 500) {
                log.warn("Redis: Slow key count operation: {}ms", countTime);
            }
            
            if (patternTime > 1000) {
                log.warn("Redis: Slow pattern search operation: {}ms", patternTime);
            }
            
        } catch (Exception e) {
            log.error("Redis: Performance metrics collection failed", e);
        }
    }
}
