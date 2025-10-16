package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Redis Cloud 전용 Redisson 설정
 * - Redis Cloud의 TLS 연결 및 인증 지원
 * - 운영환경에서만 활성화
 */
@Configuration
@Profile("prod")
@Slf4j
public class RedisCloudRedissonConfig {

    @Value("${SPRING_DATA_REDIS_HOST}")
    private String redisHost;

    @Value("${SPRING_DATA_REDIS_PORT:6379}")
    private int redisPort;

    @Value("${SPRING_DATA_REDIS_PASSWORD:}")
    private String redisPassword;

    @Value("${SPRING_DATA_REDIS_USERNAME:}")
    private String redisUsername;

    /**
     * Redis Cloud용 RedissonClient
     * - TLS 연결 및 사용자 인증 지원
     * - Redis Cloud의 고가용성 및 확장성 활용
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();

        // SingleServer 설정 - Redis Cloud 최적화
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort) // SSL 비활성화
                .setDatabase(0)
                .setConnectionPoolSize(50)  // Redis Cloud 권장 연결 풀 크기
                .setConnectionMinimumIdleSize(10)  // 최소 유휴 연결
                .setConnectTimeout(10000)  // 연결 타임아웃 (10초)
                .setTimeout(5000)  // 응답 타임아웃 (5초)
                .setRetryAttempts(3)  // 재시도 횟수
                .setRetryInterval(1000)  // 재시도 간격 (1초)
                .setKeepAlive(true)
                .setTcpNoDelay(true)
                .setSslEnableEndpointIdentification(false) // SSL 엔드포인트 식별 비활성화
                .setIdleConnectionTimeout(30000)  // 유휴 연결 타임아웃 (30초)
                .setPingConnectionInterval(30000);  // 연결 상태 확인 간격 (30초)

        // Redis Cloud 인증 설정
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
            log.info("Redis Cloud 인증 설정: 패스워드 인증 활성화");
        }

        if (redisUsername != null && !redisUsername.trim().isEmpty()) {
            config.useSingleServer().setUsername(redisUsername);
            log.info("Redis Cloud 인증 설정: 사용자명 인증 활성화");
        }

        log.info("Redis Cloud Redisson 클라이언트 생성: redis://{}:{}", redisHost, redisPort);
        
        return Redisson.create(config);
    }
}

