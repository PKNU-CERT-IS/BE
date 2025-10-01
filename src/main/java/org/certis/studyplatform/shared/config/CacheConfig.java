package org.certis.studyplatform.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Configuration
public class CacheConfig {

    /**
     * Redis Cache Manager 설정
     * application.yml 파일의 'spring.cache.type' 값이 'redis'일 때만 활성화됩니다.
     */
    @Bean("redisCacheManager")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    @ConditionalOnMissingBean(name = "fallbackCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        log.info("✅ Configuring Redis as the primary cache manager.");

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 토큰 관련 캐시 TTL 최적화 (1시간 → 30분)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues() // Null 값 캐싱 비활성화
                .computePrefixWith(cacheName -> "certis:" + cacheName + ":"); // 키 네임스페이스 추가

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * In-Memory Cache Manager (Fallback)
     * 'redisCacheManager' 빈이 없을 경우 (예: spring.cache.type이 redis가 아닐 때) 활성화됩니다.
     */
    @Bean("inMemoryCacheManager")
    @Primary
    @ConditionalOnMissingBean(name = "redisCacheManager")
    public CacheManager inMemoryCacheManager() {
        log.warn("🔄 Redis cache manager not found. Configuring in-memory cache manager as fallback.");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList("defaultCache", "userCache", "postCache")); // 필요한 캐시 이름 등록
        return cacheManager;
    }
}