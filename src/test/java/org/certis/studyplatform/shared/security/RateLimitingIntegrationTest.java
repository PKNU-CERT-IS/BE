package org.certis.studyplatform.shared.security;

import org.certis.studyplatform.config.EmbeddedRedisConfig;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate Limiting 통합 테스트
 * - 실제 Embedded Redis를 구동하여 Redisson 및 Bucket4j 동작 확인
 * - redis-test 프로필 사용 (TestRedisMockConfig 비활성화)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("redis-test") // 중요: test 프로필 대신 redis-test 사용
@Import({EmbeddedRedisConfig.class, TestEmbeddedPostgresConfig.class}) // Embedded Redis 및 DB 설정 로드
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Rate Limit 통합 테스트: 10회 허용 후 11회째 차단")
    void shouldRateLimitRealRedis() throws Exception {
        String endpoint = "/api/v1/auth/token/refresh";

        // 1. 10회 요청은 성공 (또는 인증 오류 등 Rate Limit 관련 오류가 아님)
        for (int i = 0; i < 10; i++) {
            int currentRequestIndex = i;
            mockMvc.perform(post(endpoint)
                            .header("X-Forwarded-For", "127.0.0.1")) // IP 기반 제한이므로 IP 고정
                    .andExpect(result -> {
                        // 429가 아니면 통과한 것으로 간주
                        if (result.getResponse().getStatus() == ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode()) {
                            throw new AssertionError("Rate limit exceeded prematurely at request " + (currentRequestIndex + 1));
                        }
                    });
        }

        // 2. 11번째 요청은 429 Too Many Requests 발생해야 함
        mockMvc.perform(post(endpoint)
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().is(ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode()));
    }
    
    @Test
    @DisplayName("다른 IP는 별도의 Limit을 가진다")
    void shouldSeparateLimitByIp() throws Exception {
        String endpoint = "/api/v1/auth/token/refresh";

        // IP 1: 5회 요청
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(endpoint)
                    .header("X-Forwarded-For", "10.0.0.1"));
        }
        
        // IP 2: 5회 요청 (IP 1의 영향 없어야 함)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(endpoint)
                            .header("X-Forwarded-For", "10.0.0.2"))
                    .andExpect(result -> {
                        if (result.getResponse().getStatus() == ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode()) {
                            throw new AssertionError("Rate limit affected by other IP");
                        }
                    });
        }
    }
}
