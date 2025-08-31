package org.certis.studyplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 - 실패 시 Fallback 메커니즘 포함
 */
@Configuration
@Profile({"dev", "local", "embedded"})
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * 메모리 기반 캐시 매니저 (Redis 실패 시 기본값)
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        logger.info("🔄 Using in-memory cache manager (Redis fallback)");

        // ✅ 생성자에서 직접 캐시 이름들 전달 (가장 간단한 방법)
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "users", "posts", "comments", "sessions",
                "categories", "tags", "files", "statistics"
        );

        cacheManager.setAllowNullValues(false);

        logger.info("📋 Configured {} cache names", cacheManager.getCacheNames().size());
        return cacheManager;
    }

    /**
     * Redis 연결이 가능한 경우에만 RedisTemplate 생성
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            // 연결 테스트
            connectionFactory.getConnection().ping();

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();

            logger.info("✅ Redis template configured successfully");
            return template;

        } catch (Exception e) {
            logger.warn("⚠️  Redis connection failed, using fallback cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Redis 연결 팩토리 (선택적)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            String host = System.getProperty("spring.data.redis.host", "localhost");
            int port = Integer.parseInt(System.getProperty("spring.data.redis.port", "6379"));

            LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
            factory.setValidateConnection(true);
            factory.afterPropertiesSet();

            logger.info("🔗 Redis connection factory created for {}:{}", host, port);
            return factory;

        } catch (Exception e) {
            logger.warn("⚠️  Failed to create Redis connection factory: {}", e.getMessage());
            return null;
        }
    }
}