package org.certis.studyplatform.config;

import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RKeys;
import org.redisson.api.RSet;
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
        RedissonClient mockClient = Mockito.mock(RedissonClient.class);
        
        // RAtomicLong mock 설정
        RAtomicLong mockAtomicLong = Mockito.mock(RAtomicLong.class);
        Mockito.when(mockClient.getAtomicLong(Mockito.anyString())).thenReturn(mockAtomicLong);
        
        // RSet mock 설정 (raw type으로 처리하여 타입 안전성 문제 해결)
        @SuppressWarnings("unchecked")
        RSet<Object> mockSet = Mockito.mock(RSet.class);
        Mockito.when(mockClient.getSet(Mockito.anyString())).thenReturn(mockSet);
        
        // RKeys mock 설정
        RKeys mockKeys = Mockito.mock(RKeys.class);
        Mockito.when(mockClient.getKeys()).thenReturn(mockKeys);
        
        return mockClient;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactoryMock() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactoryMock() {
        return Mockito.mock(ReactiveRedisConnectionFactory.class);
    }
}


