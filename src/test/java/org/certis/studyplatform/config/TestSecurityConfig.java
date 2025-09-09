package org.certis.studyplatform.config;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.config.TestAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 테스트 환경에서 사용되는 Security 설정
 * JWT 대신 TestAuthenticationFilter를 사용하여 간단한 Mock 인증 처리
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Profile("test") // 테스트 환경에서만 활성화
public class TestSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final TestAuthenticationFilter testAuthenticationFilter;

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // H2 Console을 위한 설정
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                // 경로별 권한 설정 - 테스트에서는 더 관대하게 설정
                .authorizeHttpRequests(auths -> auths
                        // 인증이 필요하지 않은 경로
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        // 테스트에서는 모든 API 경로 허용
                        .requestMatchers("/api/**").permitAll()
                        // 그 외 요청은 인증 필요 (Mock 인증으로 처리)
                        .anyRequest().authenticated()
                )
                // 테스트용 Mock 인증 필터 등록
                .addFilterBefore(testAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 기본 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Profile("test")
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}