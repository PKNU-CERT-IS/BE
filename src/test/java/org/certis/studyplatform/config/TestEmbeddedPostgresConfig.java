package org.certis.studyplatform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * 테스트용 Embedded PostgreSQL Configuration
 */
@TestConfiguration
public class TestEmbeddedPostgresConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestEmbeddedPostgresConfig.class);

    @Bean(destroyMethod = "close")
    @Primary
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        logger.info("🐘 Starting Test Embedded PostgreSQL...");
        
        try {
            // 고유한 데이터 디렉토리 생성 (타임스탬프 기반)
            String dataDir = System.getProperty("java.io.tmpdir") + "/embedded-postgres-test-" + System.currentTimeMillis();
            
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setPort(0) // 랜덤 포트 사용으로 충돌 방지
                    .setCleanDataDirectory(true) // 테스트에서는 깨끗한 데이터 디렉토리 사용
                    .setDataDirectory(dataDir)
                    .start();

            int actualPort = postgres.getPort();
            logger.info("✅ Test Embedded PostgreSQL started successfully on port: {}", actualPort);
            logger.info("📁 Data directory: {}", dataDir);
            
            // 연결 테스트
            String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
            logger.info("📍 JDBC URL: {}", jdbcUrl);

            return postgres;

        } catch (Exception e) {
            logger.error("❌ Failed to start Test Embedded PostgreSQL: {}", e.getMessage(), e);
            // 더 구체적인 에러 정보 제공
            if (e.getCause() != null) {
                logger.error("❌ Root cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Could not start embedded PostgreSQL for testing", e);
        }
    }

    @Bean
    @Primary
    public DataSource testDataSource(EmbeddedPostgres embeddedPostgres) {
        logger.info("🔗 Creating Test DataSource from Embedded PostgreSQL");
        
        try {
            // HikariCP 설정으로 커넥션 풀 관리 및 autoCommit 제어
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(embeddedPostgres.getJdbcUrl("postgres", "postgres"));
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            
            // 트랜잭션 제어를 위한 설정
            config.setAutoCommit(false); // autoCommit 비활성화하여 트랜잭션 롤백 가능하게 함
            config.setConnectionTimeout(60000); // 연결 타임아웃 증가
            config.setMaximumPoolSize(2); // 풀 크기 더 감소
            config.setMinimumIdle(1);
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(10000); // 검증 타임아웃 증가
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setPoolName("Test-HikariPool");
            
            // 연결 안정성을 위한 추가 설정
            config.addDataSourceProperty("socketTimeout", "30000");
            config.addDataSourceProperty("loginTimeout", "30");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("application_name", "test-app");
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("✅ Test DataSource configured successfully with autoCommit disabled");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("❌ Failed to configure Test DataSource: {}", e.getMessage(), e);
            throw new RuntimeException("Could not configure Test DataSource", e);
        }
    }

    @Bean("jooqDataSource")
    public DataSource jooqDataSource(EmbeddedPostgres embeddedPostgres) {
        logger.info("🔗 Creating jOOQ Test DataSource from Embedded PostgreSQL");
        
        try {
            // jOOQ용 DataSource 설정
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(embeddedPostgres.getJdbcUrl("postgres", "postgres"));
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            
            // jOOQ용 설정 (autoCommit=true로 트랜잭션 문제 해결)
            config.setAutoCommit(true);   // 핵심: 자동 커밋
            config.setReadOnly(false);    // 읽기/쓰기 모두 허용
            config.setConnectionTimeout(60000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setMaximumPoolSize(2); // 풀 크기 더 감소
            config.setMinimumIdle(1);
            config.setPoolName("jOOQ-Test-HikariPool");
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(10000);
            
            // 연결 안정성을 위한 추가 설정
            config.addDataSourceProperty("socketTimeout", "30000");
            config.addDataSourceProperty("loginTimeout", "30");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("application_name", "test-jooq-app");
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("✅ jOOQ Test DataSource configured successfully");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("❌ Failed to configure jOOQ Test DataSource: {}", e.getMessage(), e);
            throw new RuntimeException("Could not configure jOOQ Test DataSource", e);
        }
    }

    // Redis 관련 Bean을 Mock으로 처리
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean 
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
}