package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * 데이터베이스 초기화 서비스 (간소화된 버전)
 * - Flyway가 스키마와 기본 데이터를 모두 처리
 * - 이 서비스는 추가적인 검증과 보조 기능만 수행
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.database.verify-initialization", havingValue = "true", matchIfMissing = true)
public class DatabaseInitializationService {

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Value("${app.mock-data.enabled:false}")
    private boolean mockDataEnabled;

    private final List<String> coreTableNames = Arrays.asList(
            "member", "auth", "study", "project", "blog", "board", "schedule"
    );

    public DatabaseInitializationService(JdbcTemplate jdbcTemplate,
                                         TransactionTemplate transactionTemplate,
                                         Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    /**
     * 애플리케이션 컨텍스트 초기화 완료 후 실행
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("=== Database Initialization Verification ===");
        log.info("Active Profiles: {}", Arrays.toString(activeProfiles));
        log.info("Mock Data Enabled: {}", mockDataEnabled);

        try {
            // 1. 데이터베이스 연결 확인
            verifyDatabaseConnection();

            // 2. 핵심 테이블 존재 확인
            verifyTableExistence();

            // 3. 데이터 확인 (선택적)
            if (shouldVerifyData()) {
                verifyDataInsertion();
            }

            // 4. 시퀀스 상태 확인 (PostgreSQL)
            verifySequences();

            log.info("Database initialization verification completed successfully");

        } catch (Exception e) {
            log.error("Database initialization verification failed", e);
            // 검증 실패는 애플리케이션을 중단시키지 않음
        }
    }

    /**
     * 데이터베이스 연결 확인
     */
    private void verifyDatabaseConnection() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 'Database connection OK'", String.class);
            log.info("Database Connection: {}", result);
        } catch (Exception e) {
            log.error("Database connection failed: {}", e.getMessage());
            throw new RuntimeException("Database connection verification failed", e);
        }
    }

    /**
     * 핵심 테이블 존재 확인
     */
    private void verifyTableExistence() {
        log.info("Verifying core table existence...");

        int existingTables = 0;
        for (String tableName : coreTableNames) {
            if (tableExists(tableName)) {
                existingTables++;
                log.debug("Table '{}' exists", tableName);
            } else {
                log.warn("Table '{}' does not exist", tableName);
            }
        }

        log.info("Table verification: {}/{} core tables exist", existingTables, coreTableNames.size());

        if (existingTables == 0) {
            log.error("No core tables found! Check Flyway migration execution.");
        }
    }

    /**
     * 테이블 존재 여부 확인
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = ?)";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.debug("Error checking table existence for '{}': {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 데이터 검증이 필요한지 확인
     */
    private boolean shouldVerifyData() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(profile -> "local".equals(profile) || "dev".equals(profile));
    }

    /**
     * 데이터 삽입 확인 (local/dev 환경만)
     */
    private void verifyDataInsertion() {
        log.info("Verifying data insertion...");

        int totalRows = 0;
        for (String tableName : coreTableNames) {
            if (tableExists(tableName)) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + tableName,
                            Integer.class
                    );
                    if (count != null) {
                        totalRows += count;
                        log.info("Table '{}': {} rows", tableName, count);
                    }
                } catch (Exception e) {
                    log.warn("Could not count rows in table '{}': {}", tableName, e.getMessage());
                }
            }
        }

        log.info("Total data verification: {} rows across all core tables", totalRows);

        if (totalRows == 0 && mockDataEnabled) {
            log.warn("No data found in core tables despite mock data being enabled. Check Flyway migration files.");
        }
    }

    /**
     * 시퀀스 상태 확인 (PostgreSQL)
     */
    private void verifySequences() {
        try {
            log.info("Verifying PostgreSQL sequences...");

    String sql = """
        SELECT sequence_name, last_value, is_called
        FROM information_schema.sequences s
        JOIN pg_sequences ps ON s.sequence_name = ps.sequencename
        WHERE s.sequence_schema = 'public'
        ORDER BY sequence_name
        """;

            jdbcTemplate.query(sql, (rs, rowNum) -> {
                String sequenceName = rs.getString("sequence_name");
                long lastValue = rs.getLong("last_value");
                boolean isCalled = rs.getBoolean("is_called");

                log.debug("Sequence '{}': last_value={}, is_called={}",
                        sequenceName, lastValue, isCalled);
                return null;
            });

            log.info("Sequence verification completed");

        } catch (Exception e) {
            log.debug("Could not verify sequences (might not be PostgreSQL): {}", e.getMessage());
        }
    }

    /**
     * 수동 검증 트리거 (개발용)
     */
    public void manualVerification() {
        log.info("Manual database verification requested");
        onApplicationReady();
    }

    /**
     * 테이블별 상세 정보 조회 (개발용)
     */
    public void getTableInfo(String tableName) {
        if (!tableExists(tableName)) {
            log.warn("Table '{}' does not exist", tableName);
            return;
        }

        try {
            // 테이블 스키마 정보
            String schemaSql = """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                ORDER BY ordinal_position
                """;

            log.info("Schema for table '{}':", tableName);
            jdbcTemplate.query(schemaSql, new Object[]{tableName}, (rs, rowNum) -> {
                log.info("  Column: {} | Type: {} | Nullable: {} | Default: {}",
                        rs.getString("column_name"),
                        rs.getString("data_type"),
                        rs.getString("is_nullable"),
                        rs.getString("column_default"));
                return null;
            });

            // 행 수
            Integer rowCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName, Integer.class);
            log.info("Row count for '{}': {}", tableName, rowCount);

        } catch (Exception e) {
            log.error("Error getting table info for '{}': {}", tableName, e.getMessage());
        }
    }
}