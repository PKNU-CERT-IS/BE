package org.certis.studyplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 애플리케이션 시작 시 데이터베이스 연결 확인
 */
@Configuration
@Profile({"dev", "local"})
@Order(3)
public class ApplicationStartupConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupConfig.class);
    
    private final DataSource dataSource;

    public ApplicationStartupConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("🚀 Application startup completed!");
        verifyDatabaseConnection();
        printStartupInfo();
    }

    private void verifyDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            logger.info("✅ Database connection verified: {}", 
                connection.getMetaData().getURL());
        } catch (Exception e) {
            logger.error("❌ Database connection failed: {}", e.getMessage());
        }
    }

    private void printStartupInfo() {
        logger.info("🎯 ========================================");
        logger.info("🎯   CERT-IS Study Platform Started     ");
        logger.info("🎯 ========================================");
        logger.info("🌐 Server: http://localhost:8080");
        logger.info("📊 Database: Embedded PostgreSQL (localhost:5432)");
        logger.info("🔴 Redis: Embedded Redis (localhost:6379)");
        logger.info("📖 API Docs: http://localhost:8080/swagger-ui.html");
        logger.info("🔧 Actuator: http://localhost:8080/actuator/health");
        logger.info("🎯 ========================================");
    }
}