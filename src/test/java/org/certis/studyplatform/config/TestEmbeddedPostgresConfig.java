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
        // 랜덤 포트 사용으로 테스트 격리 및 충돌 방지
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPort(0) // 0 = 랜덤 포트 사용
                .start();
        
        System.out.println("🧪 Test Embedded PostgreSQL started on port: " + postgres.getPort());
        
        return postgres.getPostgresDatabase();
    }
} 