package org.certis.studyplatform.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 통합 테스트용 Embedded Redis 설정
 * 실제 Redis 서버 없이 메모리 내에서 Redis 동작
 */
@Slf4j
@TestConfiguration
@Profile("redis-test") // redis-test 프로필에서만 활성화
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @PostConstruct
    public void startRedis() throws IOException {
        int port = isRedisRunning() ? findAvailablePort() : redisPort;
        
        // ARM Mac 등 환경 호환성을 위해 maxheap 설정이 필요할 수 있음
        try {
            redisServer = RedisServer.builder()
                    .port(port)
                    .setting("maxmemory 128M") // 메모리 제한 설정
                    .build();
            
            redisServer.start();
            log.info("Embedded Redis started on port {}", port);
            
            // 동적으로 할당된 포트를 시스템 프로퍼티에 설정 (application-redis-test.yml에서 참조 가능)
            System.setProperty("embedded.redis.actual.port", String.valueOf(port));
            
        } catch (Exception e) {
            log.error("Embedded Redis start failed", e);
            // 이미 실행 중인 경우 무시하거나 재시도 하는 등의 로직이 필요할 수 있음
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
            log.info("Embedded Redis stopped");
        }
    }

    /**
     * 현재 설정된 포트가 사용 중인지 확인
     */
    private boolean isRedisRunning() throws IOException {
        return isPortRunning(redisPort);
    }

    private boolean isPortRunning(int port) throws IOException {
        try (java.net.Socket ignored = new java.net.Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 사용 가능한 랜덤 포트 찾기
     */
    private int findAvailablePort() throws IOException {
        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
