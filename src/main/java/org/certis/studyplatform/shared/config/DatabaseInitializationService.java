package org.certis.studyplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class DatabaseInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.core.env.Environment environment;

    @Value("${SQL_INIT_MODE:always}")
    private String sqlInitMode;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    public DatabaseInitializationService(JdbcTemplate jdbcTemplate,
                                         TransactionTemplate transactionTemplate,
                                         org.springframework.core.env.Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.environment = environment;
    }

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
            "db/V19__Insert_project_meeting.sql",
            "db/V20__Insert_schedule_attached.sql",
            "db/V21__Insert_study_meeting.sql",
            "db/V22__Insert_study_participant.sql",
            "db/V23__Insert_study_meeting_link.sql",
            "db/V24__Insert_project_meeting_link.sql"
    );

    @Override
    public void run(String... args) throws Exception {
        String actualSqlInitMode = System.getenv("SQL_INIT_MODE");
        if (actualSqlInitMode == null) {
            actualSqlInitMode = System.getProperty("SQL_INIT_MODE");
        }
        if (actualSqlInitMode == null) {
            actualSqlInitMode = environment.getProperty("SQL_INIT_MODE", "always");
        }

        logger.info("🔍 DataSource Configuration:");
        logger.info("  - Primary DataSource: autoCommit=false (JPA, Spring Boot auto-config)");
        logger.info("  - jOOQ DataSource: autoCommit=true (separate pool)");
        logger.info("  - SQL_INIT_MODE: {}", actualSqlInitMode);

        if ("always".equals(actualSqlInitMode)) {
            logger.info("🚀 Starting database initialization with mock data...");
            initializeDatabase();
        } else {
            logger.info("📋 Database initialization skipped (SQL_INIT_MODE={})", actualSqlInitMode);
        }
    }

    public void initializeDatabase() {
        try {
            logger.info("📊 Starting database initialization...");

            // 1단계: 기존 데이터 전체 삭제
            clearAllExistingData();

            // 2단계: 각 SQL 파일을 개별 트랜잭션으로 실행
            insertMockDataWithIndividualTransactions();

            // 3단계: 데이터 삽입 후 시퀀스를 실제 MAX 값으로 동기화
            syncAllSequencesToMaxValues();

            // 4단계: 시퀀스 동기화 상태 확인
            verifySequenceSynchronization();

            logger.info("✅ Database initialization completed successfully");

        } catch (Exception e) {
            logger.error("❌ Database initialization failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * 기존 데이터 전체 삭제
     */
    private void clearAllExistingData() {
        transactionTemplate.execute(status -> {
            try {
                logger.info("🗑️ Clearing all existing data...");

                // 외래키 제약조건 순서에 맞춰 삭제
                List<String> deleteStatements = Arrays.asList(
                        "DELETE FROM project_meeting_link",
                        "DELETE FROM project_meeting",
                        "DELETE FROM study_meeting_link",
                        "DELETE FROM study_participant",
                        "DELETE FROM study_meeting",
                        "DELETE FROM study_attached",
                        "DELETE FROM schedule_attached",
                        "DELETE FROM schedule_status",
                        "DELETE FROM project_participant",
                        "DELETE FROM project_attached",
                        "DELETE FROM board_view",
                        "DELETE FROM board_report",
                        "DELETE FROM board_like",
                        "DELETE FROM board_attached",
                        "DELETE FROM blog_view",
                        "DELETE FROM blog_tag",
                        "DELETE FROM schedule",
                        "DELETE FROM board",
                        "DELETE FROM blog",
                        "DELETE FROM project",
                        "DELETE FROM study",
                        "DELETE FROM member_contact",
                        "DELETE FROM member_penalty",
                        "DELETE FROM auth",
                        "DELETE FROM member"
                );

                int totalDeleted = 0;
                for (String deleteStatement : deleteStatements) {
                    try {
                        int deleted = jdbcTemplate.update(deleteStatement);
                        if (deleted > 0) {
                            totalDeleted += deleted;
                            logger.debug("🗑️ {}: {} rows deleted", deleteStatement, deleted);
                        }
                    } catch (Exception e) {
                        logger.debug("⚠️ Failed to execute {}: {}", deleteStatement, e.getMessage());
                    }
                }

                // 시퀀스 리셋 (데이터 삽입 전에 1로 리셋)
                resetAllSequences();

                logger.info("✅ Cleared {} rows of existing data", totalDeleted);
                return null;

            } catch (Exception e) {
                logger.error("❌ Failed to clear existing data", e);
                status.setRollbackOnly();
                throw new RuntimeException("Failed to clear existing data", e);
            }
        });
    }

    /**
     * 모든 시퀀스를 1로 리셋 (데이터 삽입 전)
     */
    private void resetAllSequences() {
        // 스키마에 정의된 모든 BIGSERIAL 테이블의 시퀀스들
        String[] sequences = {
                "member_id_seq",
                "study_id_seq",
                "project_id_seq",
                "schedule_id_seq",
                "blog_id_seq",
                "board_id_seq",
                "blog_tag_id_seq",
                "blog_view_id_seq",
                "board_attached_id_seq",
                "board_like_id_seq",
                "board_report_id_seq",
                "board_view_id_seq",
                "project_attached_id_seq",
                "project_participant_id_seq",
                "project_meeting_link_id_seq",
                "project_meeting_id_seq",
                "schedule_attached_id_seq",
                "schedule_status_id_seq",
                "study_attached_id_seq",
                "study_meeting_id_seq",
                "study_participant_id_seq",
                "study_meeting_link_id_seq"
        };

        logger.info("🔄 Resetting all sequences to 1...");
        for (String seq : sequences) {
            try {
                jdbcTemplate.execute("ALTER SEQUENCE IF EXISTS " + seq + " RESTART WITH 1");
                logger.debug("🔄 Reset sequence: {} → 1", seq);
            } catch (Exception e) {
                logger.debug("⚠️ Could not reset sequence {}: {}", seq, e.getMessage());
            }
        }
        logger.info("✅ All sequences reset to 1");
    }

    /**
     * 개별 트랜잭션으로 목 데이터 삽입
     */
    private void insertMockDataWithIndividualTransactions() {
        logger.info("📝 Inserting mock data with individual transactions...");

        int successCount = 0;
        int totalFiles = sqlFiles.size();

        for (String sqlFile : sqlFiles) {
            try {
                // 각 파일마다 개별 트랜잭션
                transactionTemplate.execute(status -> {
                    try {
                        executeSqlFile(sqlFile);
                        logger.debug("✅ Executed: {}", sqlFile);
                        return null;
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to execute {}: {}", sqlFile, e.getMessage());
                        status.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                });

                successCount++;
                logger.info("📄 Completed: {} ({}/{})", sqlFile, successCount, totalFiles);

            } catch (Exception e) {
                logger.warn("❌ Skipped {}: {}", sqlFile, e.getMessage());
                // 개별 파일 실패해도 다음 파일 계속 처리
            }
        }

        logger.info("✅ Mock data insertion completed: {}/{} files succeeded", successCount, totalFiles);
    }

    /**
     * 시퀀스 동기화 상태 확인 (PostgreSQL 호환성 개선)
     */
    private void verifySequenceSynchronization() {
        logger.info("🔍 Verifying sequence synchronization...");

        try {
            // PostgreSQL에서 시퀀스 목록만 가져오기 (last_value는 개별 조회 필요)
            List<String> sequenceNames = jdbcTemplate.queryForList(
                    "SELECT sequence_name " +
                            "FROM information_schema.sequences " +
                            "WHERE sequence_schema = 'public' " +
                            "ORDER BY sequence_name",
                    String.class
            );

            if (!sequenceNames.isEmpty()) {
                logger.info("📊 Current sequence values:");
                // 각 시퀀스의 현재 값을 개별적으로 조회
                for (String sequenceName : sequenceNames) {
                    try {
                        Long currentValue = jdbcTemplate.queryForObject(
                                "SELECT last_value FROM " + sequenceName,
                                Long.class
                        );
                        logger.info("  {}: current_value={}", sequenceName, currentValue);
                    } catch (Exception e) {
                        logger.info("  {}: current_value=N/A ({})", sequenceName, e.getMessage());
                    }
                }
            } else {
                // fallback: 하드코딩된 시퀀스 목록 사용
                checkIndividualSequences();
            }

            // 주요 테이블들의 MAX ID 확인
            checkTableMaxValues();

        } catch (Exception e) {
            logger.warn("⚠️ Could not get sequence list from information_schema: {}", e.getMessage());
            // fallback: 개별 시퀀스 확인
            checkIndividualSequences();
        }
    }

    /**
     * 개별 시퀀스 값 확인 (fallback 방식)
     */
    private void checkIndividualSequences() {
        logger.info("📊 Checking individual sequence values...");

        String[] sequences = {
                "member_id_seq", "study_id_seq", "project_id_seq", "schedule_id_seq",
                "blog_id_seq", "board_id_seq", "blog_tag_id_seq", "blog_view_id_seq",
                "board_attached_id_seq", "board_like_id_seq", "board_report_id_seq",
                "board_view_id_seq", "project_attached_id_seq", "project_participant_id_seq",
                "project_meeting_id_seq", "project_meeting_link_id_seq", "schedule_attached_id_seq",
                "schedule_status_id_seq", "study_attached_id_seq", "study_meeting_id_seq",
                "study_participant_id_seq", "study_meeting_link_id_seq"
        };

        for (String seq : sequences) {
            try {
                Long currentValue = jdbcTemplate.queryForObject(
                        "SELECT last_value FROM " + seq,
                        Long.class
                );
                logger.info("  {}: {}", seq, currentValue);
            } catch (Exception e) {
                logger.debug("  {}: Could not get current value ({})", seq, e.getMessage());
            }
        }
    }

    /**
     * 주요 테이블들의 MAX ID 값 확인
     */
    private void checkTableMaxValues() {
        try {
            logger.info("📈 Table MAX ID values:");

            String[] mainTables = {"member", "study", "project", "blog", "board", "project_meeting"};
            for (String tableName : mainTables) {
                try {
                    Integer maxId = jdbcTemplate.queryForObject(
                            "SELECT COALESCE(MAX(id), 0) FROM " + tableName,
                            Integer.class
                    );
                    logger.info("  {}: MAX(id) = {}", tableName, maxId);
                } catch (Exception e) {
                    logger.debug("  {}: Could not get MAX(id)", tableName);
                }
            }
        } catch (Exception e) {
            logger.debug("⚠️ Could not check table MAX values: {}", e.getMessage());
        }
    }

    /**
     * 모든 시퀀스를 테이블의 실제 최대값으로 동기화 (PostgreSQL 호환성 개선)
     */
    private void syncAllSequencesToMaxValues() {
        logger.info("🎯 Synchronizing all sequences to table MAX values...");

        try {
            transactionTemplate.execute(status -> {
                // 주요 테이블들의 시퀀스 동기화
                syncSequence("member_id_seq", "member");
                syncSequence("study_id_seq", "study");
                syncSequence("project_id_seq", "project");
                syncSequence("blog_id_seq", "blog");
                syncSequence("board_id_seq", "board");
                syncSequence("schedule_id_seq", "schedule");

                // 관계 테이블들의 시퀀스 동기화
                syncSequence("blog_tag_id_seq", "blog_tag");
                syncSequence("blog_view_id_seq", "blog_view");
                syncSequence("board_attached_id_seq", "board_attached");
                syncSequence("board_like_id_seq", "board_like");
                syncSequence("board_report_id_seq", "board_report");
                syncSequence("board_view_id_seq", "board_view");
                syncSequence("project_attached_id_seq", "project_attached");
                syncSequence("project_participant_id_seq", "project_participant");
                syncSequence("project_meeting_id_seq", "project_meeting");
                syncSequence("project_meeting_link_id_seq", "project_meeting_link");
                syncSequence("schedule_attached_id_seq", "schedule_attached");
                syncSequence("schedule_status_id_seq", "schedule_status");
                syncSequence("study_attached_id_seq", "study_attached");
                syncSequence("study_meeting_id_seq", "study_meeting");
                syncSequence("study_participant_id_seq", "study_participant");
                syncSequence("study_meeting_link_id_seq", "study_meeting_link");

                return null;
            });

            logger.info("✅ All sequences synchronized to MAX values");

        } catch (Exception e) {
            logger.error("❌ Failed to synchronize sequences: {}", e.getMessage());
        }
    }

    /**
     * 개별 시퀀스를 테이블의 MAX 값으로 동기화
     */
    private void syncSequence(String sequenceName, String tableName) {
        try {
            // 테이블의 최대 ID 조회
            Integer maxId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM " + tableName,
                    Integer.class
            );

            if (maxId != null && maxId > 0) {
                // 데이터가 있는 경우: 시퀀스를 MAX(id)로 설정하고 is_called=true
                jdbcTemplate.execute(
                        String.format("SELECT setval('%s', %d, true)", sequenceName, maxId)
                );
                logger.debug("🔧 {} → {} (next: {})", sequenceName, maxId, maxId + 1);
            } else {
                // 데이터가 없는 경우: 시퀀스를 1로 설정하고 is_called=false
                jdbcTemplate.execute(
                        String.format("SELECT setval('%s', 1, false)", sequenceName)
                );
                logger.debug("🔧 {} → 1 (next: 1, empty table)", sequenceName);
            }

        } catch (Exception e) {
            logger.debug("⚠️ Could not sync sequence {}: {}", sequenceName, e.getMessage());
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

        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty() &&
                    !trimmedStatement.startsWith("--") &&
                    !trimmedStatement.toLowerCase().startsWith("select")) {
                try {
                    jdbcTemplate.execute(trimmedStatement);
                } catch (Exception e) {
                    // SELECT 문은 로그용이므로 에러 무시
                    if (!trimmedStatement.toLowerCase().contains("select")) {
                        throw e;
                    }
                }
            }
        }
    }

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

    public void flushAllData() {
        transactionTemplate.execute(status -> {
            try {
                logger.info("🗑️ Starting database flush...");

//                List<String> tableNames = Arrays.asList(
//                        "study_meeting_link", "study_participant", "study_meeting", "study_attached",
//                        "schedule_attached", "schedule_status",
//                        "project_meeting_link", "project_participant", "project_attached", "project_meeting",
//                        "board_view", "board_report", "board_like", "board_attached",
//                        "blog_view", "blog_tag", "schedule", "board", "blog",
//                        "project", "study", "member_contact", "member_penalty",
//                        "auth", "member"
//                );
//
//                for (String tableName : tableNames) {
//                    try {
//                        jdbcTemplate.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
//                        logger.debug("🗑️ Truncated table: {}", tableName);
//                    } catch (Exception e) {
//                        logger.warn("⚠️ Failed to truncate table {}: {}", tableName, e.getMessage());
//                    }
//                }
//
                logger.info("✅ Database flush completed");
                return null;

            } catch (Exception e) {
                logger.error("❌ Failed to flush database", e);
                status.setRollbackOnly();
                throw new RuntimeException("Database flush failed", e);
            }
        });
    }

    public void reinitializeDatabase() {
        logger.info("🔄 Manual database reinitialization requested");
        initializeDatabase();
    }
}