package org.certis.studyplatform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * 테스트용 Embedded PostgreSQL Configuration
 */
@TestConfiguration
public class TestEmbeddedPostgresConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestEmbeddedPostgresConfig.class);
    private static volatile EmbeddedPostgres SHARED_INSTANCE;
    private static final Object LOCK = new Object();

    @Bean(destroyMethod = "")
    @Primary
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        if (SHARED_INSTANCE != null) {
            return SHARED_INSTANCE;
        }

        synchronized (LOCK) {
            if (SHARED_INSTANCE != null) {
                return SHARED_INSTANCE;
            }

            logger.info("Starting Test Embedded PostgreSQL (singleton, with retry)...");

            int maxAttempts = 3;
            Exception lastError = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String dataDir = System.getProperty("java.io.tmpdir") + "/embedded-postgres-test-" + System.currentTimeMillis();

                    EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                            .setPort(0)
                            .setCleanDataDirectory(true)
                            .setDataDirectory(dataDir)
                            .start();

                    int actualPort = postgres.getPort();
                    logger.info("Embedded PostgreSQL started on port {} (attempt {}/{})", actualPort, attempt, maxAttempts);
                    logger.info("Data directory: {}", dataDir);

                    String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
                    logger.info("JDBC URL: {}", jdbcUrl);

                    SHARED_INSTANCE = postgres;

                    return SHARED_INSTANCE;
                } catch (Exception e) {
                    lastError = e;
                    logger.warn("Failed to start Embedded PostgreSQL (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                    try {
                        Thread.sleep(1500L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            logger.error("Failed to start Embedded PostgreSQL after {} attempts", maxAttempts, lastError);
            if (lastError != null && lastError.getCause() != null) {
                logger.error("Root cause: {}", lastError.getCause().getMessage());
            }
            throw new RuntimeException("Could not start embedded PostgreSQL for testing", lastError);
        }
    }

    @Bean
    @Primary
    public Flyway flyway(EmbeddedPostgres embeddedPostgres) {
        String jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres");
        logger.info("Configuring Flyway with JDBC URL: {}", jdbcUrl);
        
        return Flyway.configure()
                .dataSource(jdbcUrl, "postgres", "")
                .locations("classpath:db/migration/common")
                .baselineOnMigrate(true)
                .cleanDisabled(false) // 테스트에서는 clean 허용
                .load();
    }

    @Bean
    @Primary
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        logger.info("Initializing Flyway migrations");
        try {
            // Repair first to fix any inconsistencies
            flyway.repair();
            logger.info("Flyway repair completed");
            
            // Then migrate
            int migrationsApplied = flyway.migrate().migrationsExecuted;
            logger.info("Flyway migrations completed successfully. Migrations applied: {}", migrationsApplied);
        } catch (Exception e) {
            logger.error("Flyway migration failed, attempting clean and migrate", e);
            // If migration fails, clean and try again
            flyway.clean();
            logger.info("Flyway clean completed");
            int migrationsApplied = flyway.migrate().migrationsExecuted;
            logger.info("Flyway migrations after clean completed successfully. Migrations applied: {}", migrationsApplied);
        }
        return new FlywayMigrationInitializer(flyway, null);
    }

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource testDataSource(EmbeddedPostgres embeddedPostgres, FlywayMigrationInitializer flywayMigrationInitializer) {
        logger.info("Creating Test DataSource from Embedded PostgreSQL");
        
        // Flyway 마이그레이션이 완료된 후에만 DataSource를 생성하도록 보장
        logger.info("Flyway migration initializer dependency satisfied");
        
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(embeddedPostgres.getJdbcUrl("postgres", "postgres"));
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            
            config.setAutoCommit(false);
            config.setConnectionTimeout(60000);
            config.setMaximumPoolSize(5); // 풀 크기 약간 증가
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(10000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setPoolName("Test-HikariPool");
            
            config.addDataSourceProperty("socketTimeout", "30000");
            config.addDataSourceProperty("loginTimeout", "30");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("application_name", "test-app");
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("Test DataSource configured successfully with autoCommit disabled");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("Failed to configure Test DataSource: {}", e.getMessage(), e);
            throw new RuntimeException("Could not configure Test DataSource", e);
        }
    }

    @Bean(value = "jooqDataSource", destroyMethod = "close")
    public DataSource jooqDataSource(EmbeddedPostgres embeddedPostgres, FlywayMigrationInitializer flywayMigrationInitializer) {
        logger.info("Creating jOOQ Test DataSource from Embedded PostgreSQL");
        
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(embeddedPostgres.getJdbcUrl("postgres", "postgres"));
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            
            config.setAutoCommit(true);
            config.setReadOnly(false);
            config.setConnectionTimeout(60000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setPoolName("jOOQ-Test-HikariPool");
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(10000);
            
            config.addDataSourceProperty("socketTimeout", "30000");
            config.addDataSourceProperty("loginTimeout", "30");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("application_name", "test-jooq-app");
            
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("jOOQ Test DataSource configured successfully");
            return dataSource;
            
        } catch (Exception e) {
            logger.error("Failed to configure jOOQ Test DataSource: {}", e.getMessage(), e);
            throw new RuntimeException("Could not configure jOOQ Test DataSource", e);
        }
    }
    
    /**
     * EntityManagerFactory가 Flyway 이후에 초기화되도록 명시적으로 의존성 설정
     * 이를 통해 Hibernate의 create-drop이 Flyway 스키마를 덮어쓰지 않도록 함
     */
    @Bean
    @Primary
    @DependsOn("flywayMigrationInitializer")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("testDataSource") DataSource dataSource,
            FlywayMigrationInitializer flywayMigrationInitializer) {
        logger.info("Configuring EntityManagerFactory after Flyway migration");
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.certis.studyplatform");
        
        org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter vendorAdapter = 
                new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate"); // Flyway 스키마 검증만 수행
        properties.setProperty("hibernate.physical_naming_strategy", 
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.setProperty("hibernate.implicit_naming_strategy", 
                "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl");
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");
        
        em.setJpaProperties(properties);
        
        logger.info("EntityManagerFactory configured with Hibernate ddl-auto=validate (Flyway manages schema)");
        return em;
    }

    // Redis Mock 관련 제거 - TestRedisMockConfig에서 처리됨
}