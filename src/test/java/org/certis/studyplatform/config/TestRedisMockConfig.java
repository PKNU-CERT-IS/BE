package org.certis.studyplatform.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
        Map<String, Object> bucketStore = new ConcurrentHashMap<>();
        
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

        // Idempotency 저장소가 사용하는 RBucket mock 설정
        Mockito.when(mockClient.getBucket(Mockito.anyString())).thenAnswer(invocation -> {
            String bucketKey = invocation.getArgument(0, String.class);
            @SuppressWarnings("unchecked")
            RBucket<Object> mockBucket = Mockito.mock(RBucket.class);
            Mockito.when(mockBucket.get()).thenAnswer(i -> bucketStore.get(bucketKey));
            Mockito.doAnswer(i -> {
                Object value = i.getArgument(0);
                bucketStore.put(bucketKey, value);
                return null;
            }).when(mockBucket).set(Mockito.any(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
            return mockBucket;
        });

        // Idempotency 분산 락이 사용하는 RLock mock 설정
        RLock mockLock = Mockito.mock(RLock.class);
        try {
            Mockito.when(mockLock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenReturn(true);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to configure mock lock", e);
        }
        Mockito.when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        Mockito.doNothing().when(mockLock).unlock();
        Mockito.when(mockClient.getLock(Mockito.anyString())).thenReturn(mockLock);
        
        return mockClient;
    }

    @Bean
    @Primary
    public ProxyManager<String> proxyManagerMock() {
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = Mockito.mock(ProxyManager.class);
        @SuppressWarnings("unchecked")
        RemoteBucketBuilder<String> bucketBuilder = Mockito.mock(RemoteBucketBuilder.class);
        BucketProxy bucketProxy = Mockito.mock(BucketProxy.class);

        Mockito.when(proxyManager.builder()).thenReturn(bucketBuilder);
        Mockito.when(bucketBuilder.build(Mockito.anyString(), Mockito.<Supplier<BucketConfiguration>>any()))
                .thenReturn(bucketProxy);
        Mockito.when(bucketProxy.tryConsume(Mockito.anyLong())).thenReturn(true);

        return proxyManager;
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
