package org.certis.studyplatform;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleEmbeddedPostgresTest {

    private EmbeddedPostgres postgres;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        postgres = EmbeddedPostgres.builder()
                .setPort(0) // 랜덤 포트 사용
                .start();
        dataSource = postgres.getPostgresDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void testSimpleConnection() throws SQLException {
        assertNotNull(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            System.out.println("✅ 임베드 PostgreSQL 연결 성공!");
            
            // PostgreSQL 버전 확인
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT version()")) {
                
                if (resultSet.next()) {
                    String version = resultSet.getString(1);
                    System.out.println("📌 PostgreSQL Version: " + version);
                    assertEquals(true, version.toLowerCase().contains("postgresql"));
                }
            }
        }
    }

    @Test
    void testCreateTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 테스트 테이블 생성
            statement.execute("CREATE TABLE test_users (id SERIAL PRIMARY KEY, name VARCHAR(50))");
            
            // 데이터 삽입
            statement.execute("INSERT INTO test_users (name) VALUES ('테스트 사용자')");
            
            // 데이터 조회
            try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_users")) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertEquals(1, count);
                    System.out.println("✅ 테이블 생성 및 데이터 삽입 성공! (레코드 수: " + count + ")");
                }
            }
        }
    }
} 