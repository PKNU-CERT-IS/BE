package org.certis.studyplatform.shared.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 통합 Embedded PostgreSQL Configuration
 * 
 * 로컬 개발 환경에서 사용할 Embedded PostgreSQL 설정
 * 고정 포트 5433을 사용하여 일관된 개발 환경 제공
 * 
 * 사용법:
 * 1. IDE에서 실행: 자동으로 embedded 프로필 활성화
 * 2. 수동 활성화: SPRING_PROFILES_ACTIVE=embedded 설정
 * 3. .env 파일에서 EMBEDDED_POSTGRES_PORT=5433 설정
 */
@Configuration
@Profile({"embedded", "local", "default"})
@ConditionalOnProperty(
    name = "spring.datasource.embedded.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class EmbeddedPostgresConfig {

    @Value("${EMBEDDED_POSTGRES_PORT:5433}")  // 고정 포트 5433 사용
    private int embeddedPort;

    @Value("${EMBEDDED_POSTGRES_DATABASE:certis_local}")
    private String databaseName;

    @Value("${DB_USERNAME:postgres}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Bean(destroyMethod = "close")
    @Primary
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        System.out.println("🐘 Starting Embedded PostgreSQL on fixed port: " + embeddedPort);
        
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPort(embeddedPort)  // 고정 포트 5433 사용
                .start();
        
        int actualPort = postgres.getPort();
        System.out.println("✅ Embedded PostgreSQL started successfully on port: " + actualPort);
        System.out.println("🔗 JDBC URL: " + postgres.getJdbcUrl("postgres", databaseName));
        
        return postgres;
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws Exception {
        System.out.println("🔧 Configuring DataSource for database: " + databaseName);
        System.out.println("👤 Using database user: " + dbUsername);
        
        // 1. postgres 기본 데이터베이스에 연결하여 설정 작업
        try (Connection connection = embeddedPostgres.getPostgresDatabase().getConnection()) {
            try (Statement statement = connection.createStatement()) {
                
                // 사용자가 postgres가 아닌 경우 사용자 생성
                if (!"postgres".equals(dbUsername)) {
                    var userResult = statement.executeQuery(
                        "SELECT 1 FROM pg_roles WHERE rolname = '" + dbUsername + "'"
                    );
                    if (!userResult.next()) {
                        String createUserSql = "CREATE USER " + dbUsername;
                        if (dbPassword != null && !dbPassword.isEmpty()) {
                            createUserSql += " WITH PASSWORD '" + dbPassword + "'";
                        }
                        statement.execute(createUserSql);
                        System.out.println("👤 User '" + dbUsername + "' created successfully");
                    } else {
                        System.out.println("👤 User '" + dbUsername + "' already exists");
                    }
                }
                
                // 데이터베이스 존재 확인 및 생성
                var dbResult = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'"
                );
                
                if (!dbResult.next()) {
                    String createDbSql = "CREATE DATABASE " + databaseName;
                    if (!"postgres".equals(dbUsername)) {
                        createDbSql += " OWNER " + dbUsername;
                    }
                    statement.execute(createDbSql);
                    System.out.println("📁 Database '" + databaseName + "' created successfully");
                } else {
                    System.out.println("📁 Database '" + databaseName + "' already exists");
                }
                
                // 사용자에게 데이터베이스 권한 부여
                if (!"postgres".equals(dbUsername)) {
                    statement.execute("GRANT ALL PRIVILEGES ON DATABASE " + databaseName + " TO " + dbUsername);
                    System.out.println("🔑 Granted privileges to user: " + dbUsername);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Database setup failed: " + e.getMessage());
            // 실패해도 계속 진행
        }
        
        // 2. 커스텀 데이터베이스에 연결하는 DataSource 생성
        String jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", databaseName);
        
        DataSource dataSource = DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(dbUsername)
                .password(dbPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
        
        System.out.println("✅ DataSource configured with URL: " + jdbcUrl);
        
        // 3. 연결 테스트
        try (Connection testConnection = dataSource.getConnection()) {
            System.out.println("🔌 Database connection test successful");
            
            // 현재 데이터베이스 확인
            try (Statement statement = testConnection.createStatement()) {
                var resultSet = statement.executeQuery("SELECT current_database()");
                if (resultSet.next()) {
                    String currentDb = resultSet.getString(1);
                    System.out.println("📍 Connected to database: " + currentDb);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            throw e;
        }
        
        return dataSource;
    }
}