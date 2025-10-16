package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Local/Dev 환경에서 Flyway Clean을 자동으로 실행하는 서비스
 * - 애플리케이션 시작 시 기존 스키마와 데이터를 모두 삭제
 * - 이후 Flyway가 마이그레이션을 실행하여 깨끗한 상태로 재구성
 */
@Slf4j
@Service
@Profile({"local", "dev"})  // local, dev 환경에서만 활성화
@Order(1) // 다른 초기화보다 먼저 실행
public class FlywayConfig implements CommandLineRunner {

    private final Flyway flyway;

    @Value("${app.flyway.auto-clean:false}")
    private boolean autoCleanEnabled;

    @Value("${app.flyway.clean-on-startup:false}")
    private boolean cleanOnStartup;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${spring.flyway.locations:}")
    private List<String> flywayLocations;

    public FlywayConfig(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!isCleanAllowed()) {
            log.info("Flyway clean is disabled for profile: {}", activeProfile);
            return;
        }

        if (autoCleanEnabled && cleanOnStartup) {
            performCleanAndMigrate();
        } else {
            log.info("Flyway auto-clean is disabled (auto-clean: {}, clean-on-startup: {})",
                    autoCleanEnabled, cleanOnStartup);
        }
    }

    private boolean isCleanAllowed() {
        List<String> allowedProfiles = Arrays.asList("local", "dev");
        return allowedProfiles.contains(activeProfile);
    }

    private void performCleanAndMigrate() {
        try {
            log.warn("=====================================");
            log.warn("🧹 FLYWAY CLEAN OPERATION STARTING");
            log.warn("Profile: {}", activeProfile);
            log.warn("ALL DATA WILL BE DESTROYED!");
            log.warn("=====================================");

            // 1. Clean 실행 - 모든 스키마 객체 삭제
            log.info("Step 1: Cleaning database...");
            flyway.clean();
            log.info("✅ Database cleaned successfully");

            // 2. Migrate 실행 - 스키마와 데이터 재생성
            log.info("Step 2: Running migrations...");
            log.info("Migration locations: {}", flywayLocations);

            var migrationResult = flyway.migrate();
            log.info("✅ Migrations completed successfully");
            log.info("Applied {} migrations", migrationResult.migrationsExecuted);

            // 3. 결과 검증
            verifyMigrationResult();

            log.info("=====================================");
            log.info("🎉 FLYWAY CLEAN & MIGRATE COMPLETED");
            log.info("Database is now in a fresh state");
            log.info("=====================================");

        } catch (Exception e) {
            log.error("❌ Flyway clean and migrate failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void verifyMigrationResult() {
        try {
            var info = flyway.info();
            var current = info.current();

            if (current != null) {
                log.info("Current migration version: {}", current.getVersion());
                log.info("Migration description: {}", current.getDescription());
            }

            long pendingCount = Arrays.stream(info.pending()).count();
            if (pendingCount > 0) {
                log.warn("⚠️  {} pending migrations found", pendingCount);
            } else {
                log.info("✅ All migrations are up to date");
            }

        } catch (Exception e) {
            log.warn("Could not verify migration result: {}", e.getMessage());
        }
    }

    /**
     * 수동으로 clean & migrate 실행 (개발용)
     */
    public void manualCleanAndMigrate() {
        if (!isCleanAllowed()) {
            throw new IllegalStateException("Manual clean is not allowed in profile: " + activeProfile);
        }

        log.info("Manual clean and migrate requested");
        performCleanAndMigrate();
    }

    /**
     * Clean만 실행 (마이그레이션 없이)
     */
    public void cleanOnly() {
        if (!isCleanAllowed()) {
            throw new IllegalStateException("Clean is not allowed in profile: " + activeProfile);
        }

        log.warn("🧹 Manual clean-only operation");
        flyway.clean();
        log.info("✅ Database cleaned (no migrations applied)");
    }
}