package org.certis.studyplatform.shared.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.IOException;

@Slf4j
@Configuration
public class DataSourceConfig {

    /**
     * Local 환경 전용 DataSource 설정
     */
    @Configuration
    @Profile("local")
    static class LocalDataSourceConfig {

        @Bean(destroyMethod = "close")
        public EmbeddedPostgres embeddedPostgres(@Value("${zonky.test.embedded.postgres.port:5433}") int port) throws IOException {
            log.info("Starting Embedded PostgreSQL on port {}", port);
            EmbeddedPostgres pg = EmbeddedPostgres.builder()
                    .setPort(port)
                    .start();
            log.info("Embedded PostgreSQL started successfully.");
            return pg;
        }

        @Bean
        @Primary
        @Qualifier("dataSource")
        public DataSource dataSource(EmbeddedPostgres pg) {
            log.info("Creating PRIMARY DataSource for LOCAL environment.");
            String jdbcUrl = pg.getJdbcUrl("postgres", "postgres");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            config.setAutoCommit(false);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(20000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setPoolName("Local-Primary-HikariPool");

            log.info("PRIMARY DataSource configured: {}", jdbcUrl);
            return new HikariDataSource(config);
        }

        @Bean("jooqDataSource")
        public DataSource jooqDataSource(EmbeddedPostgres pg) {
            log.info("Creating jOOQ-specific read-only DataSource for LOCAL environment.");
            String jdbcUrl = pg.getJdbcUrl("postgres", "postgres");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername("postgres");
            config.setPassword("");
            config.setDriverClassName("org.postgresql.Driver");
            config.setPoolName("jOOQ-HikariPool");
            config.setReadOnly(true);
            config.setAutoCommit(true);
            config.setMaximumPoolSize(5);

            log.info("jOOQ DataSource configured successfully.");
            return new HikariDataSource(config);
        }
    }

    /**
     * Dev/Prod 환경 전용 DataSource 설정
     */
    @Configuration
    @Profile({"dev", "prod"})
    static class ExternalDataSourceConfig {

        private final Environment environment;

        public ExternalDataSourceConfig(Environment environment) {
            this.environment = environment;
        }

        @Bean
        @Primary
        @Qualifier("dataSource")
        public DataSource dataSource(DataSourceProperties properties) {
            String[] activeProfiles = environment.getActiveProfiles();
            log.info("Creating PRIMARY DataSource for {} environment.", String.join(", ", activeProfiles));

            HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
            dataSource.setAutoCommit(false);
            dataSource.setPoolName(String.join("_", activeProfiles).toUpperCase() + "-Primary-HikariPool");
            log.info("PRIMARY DataSource configured for URL: {}", dataSource.getJdbcUrl());
            return dataSource;
        }

        @Bean("jooqDataSource")
        public DataSource jooqDataSource(DataSourceProperties properties) {
            String[] activeProfiles = environment.getActiveProfiles();
            log.info("Creating jOOQ-specific read-only DataSource for {} environment.", String.join(", ", activeProfiles));

            HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
            dataSource.setPoolName(String.join("_", activeProfiles).toUpperCase() + "-jOOQ-HikariPool");
            dataSource.setReadOnly(true);
            dataSource.setAutoCommit(true);
            log.info("jOOQ DataSource configured successfully.");
            return dataSource;
        }
    }
}