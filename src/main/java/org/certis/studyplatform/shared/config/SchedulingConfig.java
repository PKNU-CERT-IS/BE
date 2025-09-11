package org.certis.studyplatform.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 * 
 * @EnableScheduling을 통해 @Scheduled 어노테이션 기능 활성화
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 스케줄링 관련 추가 설정이 필요한 경우 이곳에 구현
}
