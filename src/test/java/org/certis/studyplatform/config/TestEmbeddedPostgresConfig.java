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
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setPort(0) // 랜덤 포트 사용으로 충돌 방지
                    .setCleanDataDirectory(true) // 테스트에서는 깨끗한 데이터 디렉토리 사용
                    .start();

            int actualPort = postgres.getPort();
            logger.info("✅ Test Embedded PostgreSQL started successfully on port: {}", actualPort);
            
            // 연결 테스트
            String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
            logger.info("📍 JDBC URL: {}", jdbcUrl);

            return postgres;

        } catch (Exception e) {
            logger.error("❌ Failed to start Test Embedded PostgreSQL: {}", e.getMessage(), e);
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
            config.setConnectionTimeout(20000);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTestQuery("SELECT 1");
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("✅ Test DataSource configured successfully with autoCommit disabled");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("❌ Failed to configure Test DataSource: {}", e.getMessage());
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
            config.setReadOnly(true);     // 읽기 전용
            config.setConnectionTimeout(15000);
            config.setIdleTimeout(200000);
            config.setMaxLifetime(900000);
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(3);
            config.setPoolName("jOOQ-Test-HikariPool");
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(3000);
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("✅ jOOQ Test DataSource configured successfully");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("❌ Failed to configure jOOQ Test DataSource: {}", e.getMessage());
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