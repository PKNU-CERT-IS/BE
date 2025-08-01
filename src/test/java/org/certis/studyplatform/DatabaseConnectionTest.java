package org.certis.studyplatform;

import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestEmbeddedPostgresConfig.class)
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testEmbeddedPostgreSQLConnection() throws SQLException {
        assertNotNull(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            
            // PostgreSQL 버전 확인
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT version()")) {
                
                if (resultSet.next()) {
                    String version = resultSet.getString(1);
                    System.out.println("PostgreSQL Version: " + version);
                    // PostgreSQL임을 확인
                    assertEquals(true, version.toLowerCase().contains("postgresql"));
                }
            }
        }
    }

    @Test
    void testCreateAndQueryTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 테스트 테이블 생성
            statement.execute("CREATE TABLE test_table (id SERIAL PRIMARY KEY, name VARCHAR(50))");
            
            // 데이터 삽입
            statement.execute("INSERT INTO test_table (name) VALUES ('Test Data')");
            
            // 데이터 조회
            try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table")) {
                if (resultSet.next()) {
                    assertEquals(1, resultSet.getInt(1));
                }
            }
        }
    }
} 