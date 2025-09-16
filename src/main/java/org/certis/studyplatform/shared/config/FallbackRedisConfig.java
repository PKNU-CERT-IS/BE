package org.certis.studyplatform.shared.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis 연결 실패 시 사용할 Fallback 설정
 * Redis가 정말로 사용 불가능할 때만 활성화됩니다.
 */
@Configuration
@Order(100) // Redis 설정들보다 나중에 실행
@ConditionalOnProperty(
        name = "spring.data.redis.enabled",
        havingValue = "false",
        matchIfMissing = false
) // Redis가 비활성화되었을 때만 실행
public class FallbackRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(FallbackRedisConfig.class);

    private final Environment environment;

    public FallbackRedisConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Fallback Cache Manager - Redis CacheManager가 없을 때만 사용
     */
    @Bean("fallbackCacheManager")
    @ConditionalOnMissingBean(name = {"redisCacheManager", "cacheManager"})
    public CacheManager fallbackCacheManager() {
        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        logger.warn("Redis CacheManager not available in {} environment", currentProfile);
        logger.info("Using fallback ConcurrentMapCacheManager (in-memory)");

        if (currentProfile.equals("prod")) {
            logger.error("PRODUCTION WARNING: Using in-memory cache instead of Redis!");
            logger.error("Please check Redis connection and restart the application");
        }

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
                "users", "posts", "comments", "sessions", "refresh-tokens", "default"
        ));
        cacheManager.setAllowNullValues(false);

        logger.info("Fallback CacheManager configured with cache names: {}",
                cacheManager.getCacheNames());

        return cacheManager;
    }

    /**
     * In-Memory Redis 대체 서비스 - RedisTemplate이 없을 때만 사용
     */
    @Bean("inMemoryRedisLikeService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public InMemoryRedisLikeService inMemoryRedisLikeService() {
        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        logger.warn("RedisTemplate not available in {} environment", currentProfile);
        logger.info("Using in-memory storage for tokens and data");

        if (currentProfile.equals("prod")) {
            logger.error("PRODUCTION WARNING: Using in-memory token storage instead of Redis!");
            logger.error("Tokens will be lost on application restart!");
        } else {
            logger.info("Data will be stored in memory - will be lost on application restart");
        }

        return new InMemoryRedisLikeService();
    }

    // InMemoryRedisLikeService 클래스는 그대로 유지...
    @Component
    public static class InMemoryRedisLikeService {
        private static final Logger logger = LoggerFactory.getLogger(InMemoryRedisLikeService.class);
        private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Long> expireMap = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public InMemoryRedisLikeService() {
            scheduler.scheduleAtFixedRate(this::cleanExpiredKeys, 1, 1, TimeUnit.MINUTES);
            logger.info("InMemoryRedisLikeService initialized with expiration scheduler");
        }

        public void set(String key, Object value) {
            store.put(key, value);
            expireMap.remove(key);
        }

        public void setWithExpire(String key, Object value, long timeoutInSeconds) {
            store.put(key, value);
            long expireTime = System.currentTimeMillis() + (timeoutInSeconds * 1000);
            expireMap.put(key, expireTime);
        }

        public Object get(String key) {
            if (isExpired(key)) {
                delete(key);
                return null;
            }
            return store.get(key);
        }

        public boolean delete(String key) {
            Object removed = store.remove(key);
            expireMap.remove(key);
            return removed != null;
        }

        public boolean hasKey(String key) {
            if (isExpired(key)) {
                delete(key);
                return false;
            }
            return store.containsKey(key);
        }

        private boolean isExpired(String key) {
            Long expireTime = expireMap.get(key);
            if (expireTime == null) {
                return false;
            }
            return System.currentTimeMillis() > expireTime;
        }

        private void cleanExpiredKeys() {
            long now = System.currentTimeMillis();
            expireMap.entrySet().removeIf(entry -> {
                if (now > entry.getValue()) {
                    store.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }

        @PreDestroy
        public void shutdown() {
            scheduler.shutdown();
        }

        public int size() {
            return store.size();
        }

        public void clear() {
            store.clear();
            expireMap.clear();
        }
    }
}