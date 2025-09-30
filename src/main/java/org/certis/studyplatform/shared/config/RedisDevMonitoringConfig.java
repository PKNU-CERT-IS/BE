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

@Configuration
@EnableScheduling
@Profile("dev")
@ConditionalOnProperty(name = "certis.redis.dev-schedule.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RedisDevMonitoringConfig {

    @Autowired
    @Lazy
    private RedissonClient redissonClient;

    // 1분 간격 연결 확인 (가벼운 버킷 write)
    @Scheduled(fixedRate = 60000)
    public void devMonitorConnection() {
        try {
            long start = System.currentTimeMillis();
            redissonClient.getBucket("certis:dev:monitor:health").set("ok");
            long ms = System.currentTimeMillis() - start;
            log.debug("[DEV] Redis ping ok: {}ms", ms);
        } catch (Exception e) {
            log.warn("[DEV] Redis ping failed", e);
        }
    }

    // 1분 간격 경량 최적화 틱 (로깅만)
    @Scheduled(fixedRate = 60000)
    public void devOptimizationTick() {
        log.info("[DEV] Redis optimization tick (lightweight)");
    }
}
