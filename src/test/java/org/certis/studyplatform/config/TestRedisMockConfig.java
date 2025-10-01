package org.certis.studyplatform.config;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 테스트 프로필에서 Redis 의존성을 모킹하여 외부 Redis 없이 통합 테스트가 동작하도록 합니다.
 */
@Configuration
@Profile("test")
public class TestRedisMockConfig {

    @Bean
    @Primary
    public RedissonClient redissonClientMock() {
        return Mockito.mock(RedissonClient.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactoryMock() {
        return Mockito.mock(RedisConnectionFactory.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactoryMock() {
        return Mockito.mock(ReactiveRedisConnectionFactory.class, Mockito.RETURNS_DEEP_STUBS);
    }
}


