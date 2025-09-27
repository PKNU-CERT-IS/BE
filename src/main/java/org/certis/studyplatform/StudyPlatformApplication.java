package org.certis.studyplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
//@EntityScan(basePackages = "org.certis.studyplatform.**.infrastructure.persistence.entity")
@EntityScan("org.certis.studyplatform")
public class StudyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyPlatformApplication.class, args);
    }
}