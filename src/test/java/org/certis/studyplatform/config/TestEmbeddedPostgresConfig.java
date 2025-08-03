package org.certis.studyplatform.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

@TestConfiguration
@Profile("test")
public class TestEmbeddedPostgresConfig {

    @Bean
    @Primary
    public DataSource testDataSource() throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(0) // 랜덤 포트 사용 (테스트 격리)
                .start()
                .getPostgresDatabase();
    }
} 