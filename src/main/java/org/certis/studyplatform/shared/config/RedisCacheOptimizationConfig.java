package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐시 최적화 설정
 * - 캐시 적중률 개선
 * - 메모리 사용량 최적화
 * - 성능 모니터링
 */
@Configuration
@EnableScheduling
@Profile("prod")
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
            log.info("Redis: Starting cache optimization");
            
            RKeys keys = redissonClient.getKeys();
            
            // 1. 만료된 키 정리
            int expiredKeysCleaned = cleanupExpiredKeys();
            
            // 2. 사용 빈도가 낮은 키 식별 및 정리
            int lowUsageKeysCleaned = cleanupLowUsageKeys();
            
            // 3. 메모리 사용량 확인
            long totalKeys = keys.count();
            long blogKeys = keys.getKeysByPattern("certis:blog:*").spliterator().getExactSizeIfKnown();
            long boardKeys = keys.getKeysByPattern("certis:board:*").spliterator().getExactSizeIfKnown();
            
            log.info("Redis Optimization Complete: Total keys: {}, Blog: {}, Board: {}, Expired cleaned: {}, Low usage cleaned: {}", 
                    totalKeys, blogKeys, boardKeys, expiredKeysCleaned, lowUsageKeysCleaned);
            
        } catch (Exception e) {
            log.error("Redis: Cache optimization failed", e);
        }
    }

    /**
     * 만료된 키 정리
     */
    private int cleanupExpiredKeys() {
        int cleanedCount = 0;
        
        try {
            RKeys keys = redissonClient.getKeys();
            
            // Blog 키들 정리
            for (String key : keys.getKeysByPattern("certis:blog:*")) {
                long ttl = keys.remainTimeToLive(key);
                if (ttl <= 0) {
                    keys.delete(key);
                    cleanedCount++;
                }
            }
            
            // Board 키들 정리
            for (String key : keys.getKeysByPattern("certis:board:*")) {
                long ttl = keys.remainTimeToLive(key);
                if (ttl <= 0) {
                    keys.delete(key);
                    cleanedCount++;
                }
            }
            
        } catch (Exception e) {
            log.error("Redis: Failed to cleanup expired keys", e);
        }
        
        return cleanedCount;
    }

    /**
     * 사용 빈도가 낮은 키 정리
     * TTL이 1시간 이하로 남은 키들을 정리
     */
    private int cleanupLowUsageKeys() {
        int cleanedCount = 0;
        
        try {
            RKeys keys = redissonClient.getKeys();
            long oneHourInMillis = TimeUnit.HOURS.toMillis(1);
            
            // Blog 조회자 목록 정리 (TTL이 1시간 이하)
            for (String key : keys.getKeysByPattern("certis:blog:view:members:*")) {
                long ttl = keys.remainTimeToLive(key);
                if (ttl > 0 && ttl <= oneHourInMillis) {
                    keys.delete(key);
                    cleanedCount++;
                }
            }
            
            // Board 조회자 목록 정리 (TTL이 1시간 이하)
            for (String key : keys.getKeysByPattern("certis:board:view:members:*")) {
                long ttl = keys.remainTimeToLive(key);
                if (ttl > 0 && ttl <= oneHourInMillis) {
                    keys.delete(key);
                    cleanedCount++;
                }
            }
            
        } catch (Exception e) {
            log.error("Redis: Failed to cleanup low usage keys", e);
        }
        
        return cleanedCount;
    }

    /**
     * 캐시 적중률 모니터링
     * 매 30분마다 실행
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1,800,000ms
    public void monitorCacheHitRate() {
        try {
            RKeys keys = redissonClient.getKeys();
            
            long totalKeys = keys.count();
            long blogKeys = keys.getKeysByPattern("certis:blog:*").spliterator().getExactSizeIfKnown();
            long boardKeys = keys.getKeysByPattern("certis:board:*").spliterator().getExactSizeIfKnown();
            
            // 활성 키 비율 계산
            double activeRatio = (double) (blogKeys + boardKeys) / totalKeys;
            
            log.info("Redis Cache Hit Rate: Total keys: {}, Active keys: {}, Active ratio: {:.2f}%", 
                    totalKeys, blogKeys + boardKeys, activeRatio * 100);
            
            // 캐시 적중률이 낮으면 경고
            if (activeRatio < 0.3) {
                log.warn("Redis: Low cache hit rate detected: {:.2f}%", activeRatio * 100);
            }
            
        } catch (Exception e) {
            log.error("Redis: Cache hit rate monitoring failed", e);
        }
    }

    /**
     * 메모리 압박 상황 대응
     * 키 개수가 임계값을 초과하면 강제 정리
     */
    @Scheduled(fixedRate = 900000) // 15분마다 실행
    public void emergencyMemoryCleanup() {
        try {
            RKeys keys = redissonClient.getKeys();
            long totalKeys = keys.count();
            
            // 임계값: 8000개 키
            if (totalKeys > 8000) {
                log.warn("Redis: Emergency memory cleanup triggered. Total keys: {}", totalKeys);
                
                // 오래된 조회자 목록 강제 정리
                int cleanedCount = 0;
                for (String key : keys.getKeysByPattern("certis:*:members:*")) {
                    long ttl = keys.remainTimeToLive(key);
                    if (ttl > 0 && ttl <= TimeUnit.HOURS.toMillis(4)) {
                        keys.delete(key);
                        cleanedCount++;
                    }
                }
                
                log.info("Redis: Emergency cleanup completed. Cleaned {} keys", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("Redis: Emergency memory cleanup failed", e);
        }
    }
}
