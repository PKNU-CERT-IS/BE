package org.certis.studyplatform.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaRepositories(basePackages = "org.certis.studyplatform")
@EnableJpaAuditing
public class JpaConfig {
}