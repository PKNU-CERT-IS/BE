package org.certis.studyplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Redis CacheManager가 없을 때만 생성되는 fallback cache
     * Bean 이름을 다르게 하고 CacheManager 타입이 아닌 구체적인 Bean을 체크
     */
    @Bean("memoryCacheManager")
    @ConditionalOnMissingBean(name = {"redisCacheManager", "cacheManager"})
    public CacheManager memoryCacheManager() {
        logger.warn("Creating fallback memory cache - Redis not available");
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "users", "posts", "comments", "sessions", "categories", "tags", 
                "files", "statistics", "auth-tokens", "configurations"
        );
        
        cacheManager.setAllowNullValues(false);
        
        logger.info("Memory cache manager configured with {} caches", 
                   cacheManager.getCacheNames().size());
        
        return cacheManager;
    }
}