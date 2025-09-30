package org.certis.studyplatform.shared.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * ElastiCache Serverless 전용 Redisson 설정
 * - AUTH 없이 TLS만으로 연결
 * - 운영환경에서만 활성화
 */
@Configuration
@Profile("prod")
public class ElastiCacheRedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(ElastiCacheRedissonConfig.class);

    @Value("${SPRING_DATA_REDIS_HOST}")
    private String redisHost;

    @Value("${SPRING_DATA_REDIS_PORT:6379}")
    private int redisPort;

    /**
     * ElastiCache Serverless용 RedissonClient
     * - AUTH 명령어 없이 TLS 연결만 수행
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();

        // SingleServer 설정 - ElastiCache Serverless (트래픽 스파이크 대응 최적화)
        config.useSingleServer()
                .setAddress("rediss://" + redisHost + ":" + redisPort)
                .setDatabase(0)
                .setConnectionPoolSize(100)  // 트래픽 스파이크 대응을 위해 연결 풀 크기 대폭 증가 (50 → 100)
                .setConnectionMinimumIdleSize(20)  // 최소 유휴 연결 증가 (10 → 20)
                .setConnectTimeout(5000)  // 연결 타임아웃 최적화 (10초 → 5초)
                .setTimeout(2000)  // 응답 타임아웃 최적화 (5초 → 2초)
                .setRetryAttempts(1)  // 재시도 횟수 감소 (2 → 1) - 빠른 실패로 트래픽 감소
                .setRetryInterval(500)  // 재시도 간격 최적화 (1초 → 0.5초)
                .setKeepAlive(true)
                .setTcpNoDelay(true)
                .setSslEnableEndpointIdentification(true)
                .setIdleConnectionTimeout(60000)  // 유휴 연결 타임아웃 증가 (30초 → 60초)
                .setPingConnectionInterval(15000);  // 연결 상태 확인 간격 최적화 (30초 → 15초)

        // 중요: 패스워드 관련 설정을 전혀 하지 않음
        // singleServerConfig.setPassword() 호출하지 않음
        // singleServerConfig.setUsername() 호출하지 않음

        log.info("ElastiCache Redisson 클라이언트 생성: rediss://{}:{}", redisHost, redisPort);

        try {
            RedissonClient client = Redisson.create(config);
            log.info("ElastiCache Redisson 연결 성공");
            return client;
        } catch (Exception e) {
            log.error("ElastiCache Redisson 연결 실패: {}", e.getMessage(), e);
            throw new RuntimeException("ElastiCache Redisson 연결 실패", e);
        }
    }

    /**
     * RedisConfig가 사용할 LettuceConnectionFactory
     * - Redisson과 별개로 Spring Data Redis용 연결 팩토리
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        // ElastiCache Serverless 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        // 패스워드 설정하지 않음

        // TLS 설정 - 올바른 메서드 체이닝
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(10000))
                .useSsl()
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(true);

        log.info("ElastiCache LettuceConnectionFactory 생성 완료");
        return factory;
    }
}