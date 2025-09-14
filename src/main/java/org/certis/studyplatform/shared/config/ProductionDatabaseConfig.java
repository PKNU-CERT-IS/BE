package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * 운영환경 전용 데이터베이스 설정
 * - 빈 데이터베이스: Entity 기반 스키마 생성 → Flyway baseline
 * - 기존 데이터베이스: Flyway 마이그레이션만 실행
 */
@Slf4j
@Configuration
@Profile("prod")
public class ProductionDatabaseConfig {

    @Autowired
    private Environment env;

    @Autowired
    private DataSource dataSource;

    private volatile boolean databaseInitialized = false;

    /**
     * 운영환경에서 데이터베이스 상태에 따른 동적 Hibernate 설정
     */
    @Bean
    @Profile("prod")
    public HibernatePropertiesCustomizer productionHibernateCustomizer() {
        return (properties) -> {
            try {
                if (isDatabaseEmpty()) {
                    log.info("🚀 Empty production database detected. Configuring Hibernate for schema creation...");
                    
                    // 빈 데이터베이스의 경우: Entity 기반 스키마 생성
                    properties.put("hibernate.hbm2ddl.auto", "create");
                    properties.put("hibernate.show_sql", "true");
                    properties.put("hibernate.format_sql", "true");
                    properties.put("hibernate.use_sql_comments", "true");
                    
                } else {
                    log.info("📊 Existing production database detected. Configuring Hibernate for validation only...");
                    
                    // 기존 데이터베이스의 경우: 검증만
                    properties.put("hibernate.hbm2ddl.auto", "validate");
                    properties.put("hibernate.show_sql", "false");
                    properties.put("hibernate.format_sql", "false");
                }
                
                // 공통 운영 설정
                properties.put("hibernate.connection.provider_disables_autocommit", "true");
                properties.put("hibernate.jdbc.batch_size", "25");
                properties.put("hibernate.jdbc.batch_versioned_data", "true");
                
            } catch (Exception e) {
                log.error("❌ Failed to determine database state, defaulting to validate mode", e);
                properties.put("hibernate.hbm2ddl.auto", "validate");
            }
        };
    }

    /**
     * 애플리케이션 시작 후 Flyway 처리 (운영환경: 스키마만 생성, Mock 데이터 없음)
     */
    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefresh() {
        if (databaseInitialized) {
            return; // 중복 실행 방지
        }
        
        try {
            if (isDatabaseEmpty()) {
                // 이 시점에서는 이미 Hibernate가 테이블 구조만 생성했음
                log.info("✅ JPA entities have created the SCHEMA-ONLY structure (NO mock data inserted)");
                logCreatedTables();
                createFlywayBaseline();
            } else {
                log.info("📊 Running Flyway migrations on existing database...");
                runFlywayMigrations();
            }
            
            databaseInitialized = true;
            log.info("🎯 Production database initialization completed successfully");
            
        } catch (Exception e) {
            log.error("❌ Database initialization failed", e);
            throw new RuntimeException("Production database initialization failed", e);
        }
    }

    /**
     * 생성된 테이블 목록 로깅 (운영환경 확인용)
     */
    private void logCreatedTables() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            log.info("📋 Created tables in production database:");
            
            try (ResultSet tables = metaData.getTables(
                conn.getCatalog(), "public", "%", new String[]{"TABLE"})) {
                
                int tableCount = 0;
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (!tableName.startsWith("flyway_") && 
                        !tableName.startsWith("pg_") && 
                        !tableName.startsWith("information_schema")) {
                        log.info("   ✓ {}", tableName);
                        tableCount++;
                    }
                }
                log.info("📊 Total {} business tables created (excluding system tables)", tableCount);
            }
            
        } catch (SQLException e) {
            log.warn("Could not list created tables: {}", e.getMessage());
        }
    }

    /**
     * 데이터베이스가 비어있는지 확인
     */
    private boolean isDatabaseEmpty() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 사용자 테이블 확인 (시스템 테이블 제외)
            try (ResultSet tables = metaData.getTables(
                conn.getCatalog(), 
                "public", 
                "%", 
                new String[]{"TABLE"})) {
                
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    // Flyway 메타데이터 테이블과 시스템 테이블 제외
                    if (!tableName.startsWith("flyway_") && 
                        !tableName.startsWith("pg_") && 
                        !tableName.startsWith("information_schema")) {
                        log.debug("Found existing user table: {}", tableName);
                        return false;
                    }
                }
            }
            
            log.info("No user tables found in database - considering as empty");
            return true;
        }
    }

    /**
     * 초기 Flyway 베이스라인 생성
     */
    private void createFlywayBaseline() {
        log.info("Creating Flyway baseline for future migrations...");
        
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.0")
                .baselineDescription("Initial schema created from JPA entities")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();

            // 베이스라인이 이미 있는지 확인
            var info = flyway.info();
            if (info.all().length == 0) {
                flyway.baseline();
                log.info("✅ Flyway baseline created successfully (version: 1.0)");
            } else {
                log.info("✅ Flyway baseline already exists");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to create Flyway baseline", e);
            throw new RuntimeException("Flyway baseline creation failed", e);
        }
    }

    /**
     * Flyway 마이그레이션 실행
     */
    private void runFlywayMigrations() {
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();

            // 현재 상태 확인
            var info = flyway.info();
            log.info("Current Flyway status: {} migrations, {} pending", 
                info.all().length, info.pending().length);

            // 마이그레이션 실행
            flyway.migrate();
            
        } catch (Exception e) {
            log.error("❌ Failed to run Flyway migrations", e);
            throw new RuntimeException("Flyway migration failed", e);
        }
    }
}