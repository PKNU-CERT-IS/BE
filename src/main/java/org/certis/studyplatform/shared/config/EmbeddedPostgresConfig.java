package org.certis.studyplatform.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 개선된 Embedded PostgreSQL Configuration
 *
 * 주요 개선사항:
 * - 포트 충돌 자동 해결
 * - 안정적인 예외 처리
 * - 적절한 로깅
 * - 단순화된 설정
 * - autoCommit=false 설정
 * - 종료 순서 제어로 연결 문제 해결
 */
@Configuration
@Profile({"embedded", "local", "default"})
@ConditionalOnProperty(
        name = "spring.datasource.embedded.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Order(100) // DatabaseInitializationService보다 늦게 종료되도록 설정
public class EmbeddedPostgresConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedPostgresConfig.class);

    @Value("${EMBEDDED_POSTGRES_PORT:5433}")
    private int preferredPort;

    @Value("${EMBEDDED_POSTGRES_DATABASE:certis_local}")
    private String databaseName;

    @Value("${DB_USERNAME:postgres}")
    private String dbUsername;

    @Value("${DB_PASSWORD:postgres}")
    private String dbPassword;

    // Hikari Connection Pool 설정값들
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    // EmbeddedPostgres 인스턴스를 필드로 보관하여 종료 순서 제어
    private EmbeddedPostgres embeddedPostgres;

    @Bean
    @Primary
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        logger.info("Starting Embedded PostgreSQL...");

        // 사용 가능한 포트 찾기
        int availablePort = findAvailablePort(preferredPort);

        try {
            embeddedPostgres = EmbeddedPostgres.builder()
                    .setPort(availablePort)
                    .setCleanDataDirectory(false) // 데이터 디렉토리 유지
                    .start();

            int actualPort = embeddedPostgres.getPort();
            logger.info("Embedded PostgreSQL started successfully on port: {}", actualPort);
            logger.info("Database URL: {}", embeddedPostgres.getJdbcUrl("postgres", "postgres"));

            // 포트가 변경된 경우 시스템 프로퍼티 업데이트
            if (actualPort != preferredPort) {
                updateSystemProperties(actualPort);
                logger.warn("Port changed from {} to {} due to conflict", preferredPort, actualPort);
            }

            return embeddedPostgres;

        } catch (IOException e) {
            logger.error("Failed to start Embedded PostgreSQL: {}", e.getMessage());
            throw new RuntimeException("Could not start embedded PostgreSQL", e);
        }
    }

    @Bean
    @Primary
    public DataSource embeddedDataSource(EmbeddedPostgres embeddedPostgres) {
        logger.info("Creating DataSource from Embedded PostgreSQL with autoCommit=false");

        try {
            // Hikari DataSource 설정으로 autoCommit=false 명시적 설정
            HikariConfig config = new HikariConfig();

            // 기본 연결 정보
            config.setJdbcUrl(embeddedPostgres.getJdbcUrl("postgres", dbUsername));
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName("org.postgresql.Driver");

            // 커넥션 풀 설정
            config.setPoolName("EmbeddedPostgresHikariPool");
            config.setMaximumPoolSize(maximumPoolSize);
            config.setMinimumIdle(minimumIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            config.setLeakDetectionThreshold(leakDetectionThreshold);

            // 중요: autoCommit을 false로 설정
            config.setAutoCommit(false);

            // 연결 테스트 쿼리
            config.setConnectionTestQuery("SELECT 1");

            // PostgreSQL 최적화 설정
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            HikariDataSource dataSource = new HikariDataSource(config);

            // 연결 테스트
            validateConnection(dataSource);

            logger.info("DataSource configured successfully with autoCommit=false");
            logger.info("Pool Name: {}", config.getPoolName());
            logger.info("AutoCommit: {}", config.isAutoCommit());
            logger.info("Max Pool Size: {}", config.getMaximumPoolSize());

            return dataSource;

        } catch (Exception e) {
            logger.error("Failed to configure DataSource: {}", e.getMessage());
            throw new RuntimeException("Could not configure DataSource", e);
        }
    }

    /**
     * 애플리케이션 종료 시 EmbeddedPostgres를 수동으로 종료
     * DatabaseInitializationService보다 늦게 실행되도록 함
     */
    @PreDestroy
    public void cleanup() {
        if (embeddedPostgres != null) {
            try {
                logger.info("Shutting down Embedded PostgreSQL...");
                embeddedPostgres.close();
                logger.info("Embedded PostgreSQL shutdown completed");
            } catch (Exception e) {
                logger.warn("Error during Embedded PostgreSQL shutdown: {}", e.getMessage());
            }
        }
    }

    /**
     * 사용 가능한 포트 찾기
     */
    private int findAvailablePort(int startPort) {
        for (int port = startPort; port <= startPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new RuntimeException("No available port found starting from " + startPort);
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
     * 시스템 프로퍼티 업데이트 (포트 변경 시)
     */
    private void updateSystemProperties(int actualPort) {
        String newUrl = String.format("jdbc:postgresql://localhost:%d/postgres", actualPort);
        System.setProperty("spring.datasource.url", newUrl);
        System.setProperty("EMBEDDED_POSTGRES_PORT", String.valueOf(actualPort));

        logger.info("Updated system properties for port: {}", actualPort);
    }

    /**
     * 데이터베이스 연결 검증
     */
    private void validateConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                logger.info("Database connection validated successfully");
                logger.info("Connection AutoCommit: {}", connection.getAutoCommit());

                // 데이터베이스 정보 로깅
                var metaData = connection.getMetaData();
                logger.debug("Database: {} {}",
                        metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion());
                logger.debug("URL: {}", metaData.getURL());

            } else {
                throw new SQLException("Connection validation failed");
            }
        } catch (SQLException e) {
            logger.error("Database connection validation failed: {}", e.getMessage());
            throw new RuntimeException("Database connection validation failed", e);
        }
    }
}