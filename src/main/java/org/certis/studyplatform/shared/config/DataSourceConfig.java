package org.certis.studyplatform.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.datasource.embedded.enabled", havingValue = "false", matchIfMissing = true)
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * JPA 전용 Primary DataSource
     */
    @Primary
    @Bean("dataSource")
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // JPA용 설정 (트랜잭션 관리를 위해 autoCommit=false)
        config.setAutoCommit(false);  // JPA 트랜잭션 관리
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setMaximumPoolSize(20);  // JPA는 더 많은 커넥션 필요
        config.setMinimumIdle(5);
        config.setPoolName("Primary-HikariPool");

        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    /**
     * jOOQ 전용 DataSource (기존 설정 유지)
     */
    @Bean("jooqDataSourcePool") // 빈 이름만 변경하여 충돌 방지
    public DataSource jooqDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // jOOQ용 설정 (autoCommit=true로 트랜잭션 문제 해결)
        config.setAutoCommit(true);   // 핵심: 자동 커밋
        config.setReadOnly(true);     // 읽기 전용
        config.setConnectionTimeout(15000);
        config.setIdleTimeout(200000);
        config.setMaxLifetime(900000);
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(3);
        config.setPoolName("jOOQ-HikariPool");

        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);

        return new HikariDataSource(config);
    }

    // DSLContext는 별도의 JooqConfig에서 정의하므로 여기서는 제거
}