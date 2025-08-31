package org.certis.studyplatform.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * jOOQ 전용 DataSource만 별도 생성
     * JPA는 application.yml의 기본 datasource 설정 사용
     */
    @Bean("jooqDataSource")
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
}