package org.certis.studyplatform.shared.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 'local' 또는 'test' 프로필이 활성화될 때 Embedded Redis 서버를 설정합니다.
 */
@Configuration
@Profile({"local", "test"})
public class EmbeddedRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRedisConfig.class);

    @Value("${spring.data.redis.port}")
    private int redisPort;

    private RedisServer redisServer;

    /**
     * Spring 컨텍스트가 초기화된 후 Embedded Redis 서버를 시작합니다.
     */
    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 256M")
                .build();
        try {
            redisServer.start();
            logger.info("✅ Embedded Redis server started on port {}", redisPort);
        } catch (Exception e) {
            logger.error("⚠️ Failed to start Embedded Redis server: {}", e.getMessage());
        }
    }

    /**
     * Spring 컨텍스트가 종료되기 전에 Embedded Redis 서버를 중지합니다.
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            logger.info("⏹️ Embedded Redis server stopped.");
        }
    }
}

