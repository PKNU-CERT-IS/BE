package org.certis.studyplatform.shared.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bucket4j Rate Limiting 설정
 * - Redis를 백엔드로 사용하는 분산 Rate Limiting 구성
 * - Redisson 클라이언트를 사용하여 Redis 연결
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Bucket4jConfig {

    private final RedissonClient redissonClient;

    /**
     * Bucket4j ProxyManager 생성
     * - Redis 기반 분산 Rate Limiting을 위한 핵심 컴포넌트
     * - 버킷은 Redis에 저장되어 여러 서버 인스턴스 간 공유됨
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager() {
        log.info("Initializing Bucket4j ProxyManager with Redisson backend");

        // RedissonClient를 Redisson으로 캐스팅하여 CommandExecutor 가져오기
        Redisson redisson = (Redisson) redissonClient;

        return RedissonBasedProxyManager.builderFor(redisson.getCommandExecutor())
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(5) // 버킷이 사용되지 않으면 5분 후 Redis에서 자동 삭제
                        )
                )
                .build();
    }
}
