package org.certis.studyplatform.shared.config;

import lombok.RequiredArgsConstructor;
// import org.certis.studyplatform.shared.security.JwtAuthenticationFilter;  // 🔥 JWT 비활성화
// import org.certis.studyplatform.shared.security.JwtTokenProvider;        // 🔥 JWT 비활성화
import org.certis.studyplatform.shared.security.JwtAuthenticationFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
@RequiredArgsConstructor
@Profile("!test") // 테스트 환경이 아닐 때만 활성화
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF 비활성화 (JWT 사용시 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용하지 않음 (JWT는 Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // H2 Console을 위한 설정 (개발환경용)
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                // 경로별 권한 설정
                .authorizeHttpRequests(auths -> auths
                        // 인증이 필요하지 않은 경로
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",     // 회원가입
                                "/api/v1/auth/refresh",      // 토큰 갱신
                                "/api/v1/blog",              // 블로그 목록 조회
                                "/api/v1/blog/detail",       // 블로그 상세 조회
                                "/api/v1/blog/search",       // 블로그 검색
                                "/api/v1/blog/search/keyword", // 블로그 고급 검색
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",          // 헬스체크
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 기본 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Profile("!test") // 테스트 환경이 아닐 때만 활성화
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}