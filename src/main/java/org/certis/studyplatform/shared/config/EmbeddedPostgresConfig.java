package org.certis.studyplatform.shared.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@Profile("embedded") // 'local' 대신 'embedded' 프로파일로 변경
public class EmbeddedPostgresConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "embedded")
    public DataSource dataSource() throws IOException {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPort(5433) // 기본 PostgreSQL 포트와 충돌 방지
                .start();

        return postgres.getPostgresDatabase();
    }
}