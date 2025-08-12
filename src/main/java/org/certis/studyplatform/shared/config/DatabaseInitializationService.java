package org.certis.studyplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * 데이터베이스 초기화 및 정리 서비스
 * 
 * 기능:
 * 1. 애플리케이션 시작 시 목 데이터 삽입
 * 2. 애플리케이션 종료 시 데이터 flush (선택적)
 * 3. 트랜잭션을 통한 안전한 데이터 처리
 */
@Service
public class DatabaseInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Value("${SQL_INIT_MODE:always}")
    private String sqlInitMode;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Autowired
    private org.springframework.core.env.Environment environment;

    // SQL 파일 실행 순서 (의존성 고려)
    private final List<String> sqlFiles = Arrays.asList(
        "db/V2__Insert_member_data.sql",
        "db/V3__Insert_auth_data.sql",
        "db/V4__Insert_member_penalty_data.sql",
        "db/V5__Insert_member_contact_data.sql",
        "db/V6__Insert_study_data.sql",
        "db/V7__Insert_project_data.sql",
        "db/V8__Insert_blog_data.sql",
        "db/V9__Insert_board_data.sql",
        "db/V10__Insert_schedule_data.sql",
        "db/V11__Insert_blog_tag.sql",
        "db/V12__Insert_blog_view.sql",
        "db/V13__Insert_board_attached.sql",
        "db/V14__Insert_board_like.sql",
        "db/V15__Insert_board_report.sql",
        "db/V16__Insert_board_view.sql",
        "db/V17__Insert_project_attached.sql",
        "db/V18__Insert_project_participant.sql",
        "db/V19__Insert_project_tag.sql",
        "db/V20__Insert_schedule_attached.sql",
        "db/V21__Insert_study_meeting.sql",
        "db/V22__Insert_study_participant.sql",
        "db/V23__Insert_study_tag.sql"
    );

    @Override
    public void run(String... args) throws Exception {
        // 환경변수를 직접 확인 (우선순위: 시스템 환경변수 > 시스템 프로퍼티 > @Value)
        String actualSqlInitMode = System.getenv("SQL_INIT_MODE");
        if (actualSqlInitMode == null) {
            actualSqlInitMode = System.getProperty("SQL_INIT_MODE");
        }
        if (actualSqlInitMode == null) {
            actualSqlInitMode = environment.getProperty("SQL_INIT_MODE", "always");
        }
        
        logger.info("🔍 Environment check:");
        logger.info("  - System.getenv('SQL_INIT_MODE'): {}", System.getenv("SQL_INIT_MODE"));
        logger.info("  - System.getProperty('SQL_INIT_MODE'): {}", System.getProperty("SQL_INIT_MODE"));
        logger.info("  - @Value sqlInitMode: {}", sqlInitMode);
        logger.info("  - environment.getProperty(): {}", environment.getProperty("SQL_INIT_MODE"));
        logger.info("  - Final decision: {}", actualSqlInitMode);
        
        if ("always".equals(actualSqlInitMode)) {
            logger.info("🚀 Starting database initialization with mock data...");
            initializeDatabase();
        } else {
            logger.info("📋 Database initialization skipped (SQL_INIT_MODE={})", actualSqlInitMode);
        }
    }

    /**
     * 데이터베이스 초기화 및 목 데이터 삽입
     */
    @Transactional
    public void initializeDatabase() {
        try {
            logger.info("📊 Inserting mock data in transaction...");
            
            for (String sqlFile : sqlFiles) {
                try {
                    executeSqlFile(sqlFile);
                    logger.debug("✅ Executed: {}", sqlFile);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to execute {}: {}", sqlFile, e.getMessage());
                    // 개별 파일 실패해도 계속 진행
                }
            }
            
            logger.info("✅ Database initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Database initialization failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * SQL 파일 실행
     */
    private void executeSqlFile(String sqlFilePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(sqlFilePath);
        if (!resource.exists()) {
            logger.warn("📄 SQL file not found: {}", sqlFilePath);
            return;
        }

        String sql = FileCopyUtils.copyToString(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );

        // 세미콜론으로 구분된 SQL 문 실행
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
                jdbcTemplate.execute(trimmedStatement);
            }
        }
    }

    /**
     * 애플리케이션 종료 시 데이터 flush
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        if ("create-drop".equals(ddlAuto)) {
            logger.info("🧹 Application shutdown detected - JPA will handle table cleanup");
            return;
        }

        try {
            logger.info("🧹 Flushing database data on application shutdown...");
            flushAllData();
            logger.info("✅ Database flush completed successfully");
        } catch (Exception e) {
            logger.error("❌ Database flush failed", e);
        }
    }

    /**
     * 모든 테이블 데이터 삭제 (테이블 구조는 유지)
     */
    @Transactional
    public void flushAllData() {
        try {
            logger.info("🗑️ Starting database flush...");
            
            // PostgreSQL에서는 CASCADE를 사용하여 외래키 제약조건을 처리
            List<String> tableNames = Arrays.asList(
                "study_tag", "study_participant", "study_meeting", "schedule_attached",
                "project_tag", "project_participant", "project_attached", 
                "board_view", "board_report", "board_like", "board_attached",
                "blog_view", "blog_tag", "schedule", "board", "blog", 
                "project", "study", "member_contact", "member_penalty", 
                "auth", "member"
            );
            
            // 역순으로 테이블 데이터 삭제 (의존성 순서 고려)
            for (String tableName : tableNames) {
                try {
                    jdbcTemplate.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
                    logger.debug("🗑️ Truncated table: {}", tableName);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to truncate table {}: {}", tableName, e.getMessage());
                    // 개별 테이블 실패해도 계속 진행
                }
            }
            
            logger.info("✅ Database flush completed");
            
        } catch (Exception e) {
            logger.error("❌ Failed to flush database", e);
            throw new RuntimeException("Database flush failed", e);
        }
    }

    /**
     * 수동 데이터 초기화 (개발/테스트용)
     */
    public void reinitializeDatabase() {
        logger.info("🔄 Manual database reinitialization requested");
        flushAllData();
        initializeDatabase();
    }
} 