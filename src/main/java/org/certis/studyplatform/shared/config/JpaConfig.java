package org.certis.studyplatform.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "org.certis.studyplatform")
public class JpaConfig {
    // Spring Boot가 자동으로 EntityManagerFactory와 TransactionManager 생성
    // Primary DataSource를 자동으로 사용
}