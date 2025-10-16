package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

/**
 * Local 환경 전용 Embedded Redis 설정
 * - 포트 충돌 자동 해결
 * - 안전한 시작/종료 처리
 * - Fallback 메커니즘
 */
@Slf4j
@Component
@Profile({"local", "test", "redis-test"})
@Order(1) // Redis 관련 다른 빈들보다 먼저 실행
@ConditionalOnProperty(name = "embedded.redis.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedRedisConfig {

    @Value("${embedded.redis.port:6379}")
    private int redisPort;

    @Value("${embedded.redis.maxmemory:128M}")
    private String maxMemory;

    private RedisServer redisServer;
    private volatile boolean isStarted = false;

    @PostConstruct
    public void startEmbeddedRedis() {
        try {
            log.info("🚀 Starting Embedded Redis server...");

            // 1. 포트 사용 가능 여부 확인 및 대안 포트 찾기
            int availablePort = findAvailablePort(redisPort);
            if (availablePort != redisPort) {
                log.warn("Port {} is in use, using alternative port {}", redisPort, availablePort);
                redisPort = availablePort;
                // 시스템 프로퍼티로 다른 설정에서 참조할 수 있도록 설정
                System.setProperty("embedded.redis.actual.port", String.valueOf(redisPort));
                // Spring Redis 설정에 포트를 반영하여 연결 팩토리가 올바른 포트를 사용하도록 설정
                System.setProperty("spring.data.redis.port", String.valueOf(redisPort));
                System.setProperty("spring.data.redis.host", "localhost");
            }

            // 2. Redis 프로세스가 이미 실행 중인지 확인
            if (isRedisRunningOnPort(redisPort)) {
                log.info("Redis is already running on port {}, skipping embedded Redis startup", redisPort);
                isStarted = true;
                return;
            }

            // 3. 기존 Redis 프로세스 정리 (필요시)
            cleanupExistingRedisProcesses();

            // 4. Embedded Redis 서버 생성 및 시작
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory " + maxMemory)
                    .setting("maxmemory-policy allkeys-lru")
                    .setting("timeout 0")
                    .setting("tcp-keepalive 60")
                    .setting("save \"\"") // RDB 저장 비활성화 (개발용)
                    .build();

            redisServer.start();
            isStarted = true;

            // 시작 확인을 위한 짧은 대기
            Thread.sleep(1000);

            log.info("✅ Embedded Redis started successfully on port {} (maxmemory: {})", redisPort, maxMemory);

            // 5. 연결 테스트
            testRedisConnection();

        } catch (Exception e) {
            log.error("❌ Failed to start Embedded Redis server: {}", e.getMessage(), e);
            isStarted = false;
            handleRedisStartupFailure(e);
        }
    }

    /**
     * 사용 가능한 포트 찾기
     */
    private int findAvailablePort(int preferredPort) {
        // 먼저 선호 포트 확인
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }

        // 6379-6389 범위에서 사용 가능한 포트 찾기
        for (int port = 6379; port <= 6389; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }

        // 6379 기본 Redis 포트도 확인
        if (isPortAvailable(6379)) {
            return 6379;
        }

        // 동적 포트 할당
        try (ServerSocket socket = new ServerSocket(0)) {
            int dynamicPort = socket.getLocalPort();
            log.info("Using dynamic port: {}", dynamicPort);
            return dynamicPort;
        } catch (IOException e) {
            log.warn("Failed to find available port dynamically, using default");
            return preferredPort;
        }
    }

    /**
     * 포트 사용 가능 여부 확인
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Redis가 해당 포트에서 실행 중인지 확인
     */
    private boolean isRedisRunningOnPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                pb.command("netstat", "-an");
            } else {
                pb.command("lsof", "-i", ":" + port);
            }

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                        .anyMatch(line -> line.contains(":" + port) &&
                                (line.contains("LISTEN") || line.contains("redis")));
            }
        } catch (Exception e) {
            log.debug("Could not check if Redis is running on port {}: {}", port, e.getMessage());
            return false;
        }
    }

    /**
     * 기존 Redis 프로세스 정리
     */
    private void cleanupExistingRedisProcesses() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                // Unix/Linux/Mac에서 기존 Redis 프로세스 확인
                ProcessBuilder pb = new ProcessBuilder("pgrep", "-f", "redis");
                Process process = pb.start();
                if (process.waitFor() == 0) {
                    log.debug("Found existing Redis processes, they will be left running");
                }
            }
        } catch (Exception e) {
            log.debug("Could not cleanup existing Redis processes: {}", e.getMessage());
        }
    }

    /**
     * Redis 연결 테스트
     */
    private void testRedisConnection() {
        try {
            // 간단한 소켓 연결 테스트
            try (java.net.Socket socket = new java.net.Socket("localhost", redisPort)) {
                socket.setSoTimeout(5000);
                log.debug("Redis connection test completed successfully on port {}", redisPort);
            }
        } catch (Exception e) {
            log.warn("Redis connection test failed on port {}: {}", redisPort, e.getMessage());
        }
    }

    /**
     * Redis 시작 실패 처리
     */
    private void handleRedisStartupFailure(Exception e) {
        log.warn("💡 Embedded Redis startup failed. Application will continue with fallback configuration.");
        log.warn("💡 Consider using external Redis or check port availability.");
        log.warn("💡 To disable embedded Redis, set 'embedded.redis.enabled=false' in application.yml");

        // Redis 서버 정리
        if (redisServer != null) {
            try {
                redisServer.stop();
                redisServer = null;
            } catch (Exception stopEx) {
                log.debug("Error stopping failed Redis server: {}", stopEx.getMessage());
            }
        }

        // Redis 사용 불가 상태로 마킹
        System.setProperty("embedded.redis.failed", "true");
    }

    @PreDestroy
    public void stopEmbeddedRedis() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                log.info("🔄 Stopping Embedded Redis server...");
                redisServer.stop();
                isStarted = false;
                log.info("✅ Embedded Redis stopped successfully");
            } catch (Exception e) {
                log.warn("⚠️ Error stopping Embedded Redis: {}", e.getMessage());
            } finally {
                redisServer = null;
            }
        }
    }

    /**
     * 수동 재시작 (개발용)
     */
    public void restartRedis() {
        log.info("Manual Redis restart requested");
        stopEmbeddedRedis();
        startEmbeddedRedis();
    }

    /**
     * Redis 서버 상태 확인
     */
    public boolean isRedisActive() {
        return isStarted && (redisServer == null || redisServer.isActive());
    }

    /**
     * 현재 Redis 포트 반환
     */
    public int getRedisPort() {
        return redisPort;
    }
}