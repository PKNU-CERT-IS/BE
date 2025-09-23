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
                                "/api/v1/member/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",  // 회원가입 추가
                                "/api/v1/auth/refresh",   // 토큰 갱신
                                "/api/v1/blog",              // 블로그 목록 조회
                                "/api/v1/blog/detail",       // 블로그 상세 조회
                                "/api/v1/blog/search",       // 블로그 검색
                                "/api/v1/blog/search/keyword", // 블로그 고급 검색
                
                                
                                //문서 모니터링
                                "/api/v1/board",
                                "/api/v1/board/search/**",
                                "/api/v1/board/search",
                                "/api/v1/board/detail",
                
                                "/api/v1/project/search",
                                "/api/v1/project/detail",
                                "/api/v1/project/search/**",
                                "/api/v1/project",
                                "/api/v1/project/{projectId}/meetings",
                                "/api/v1/project/meeting/detail",
                                "/api/v1/project/meeting/all",
                                "/api/v1/project/participant/{projectId}/participants/{participantId}",
                                "/api/v1/project/participant/members/{memberId}/participants",
                                "/api/v1/project/participant/{projectId}/participants/all",
                                "/api/v1/project/participant/{projectId}/participants/pending",
                                "/api/v1/project/participant/{projectId}/participants/approved",
                
                                "/api/v1/schedule/requests",
                                "/api/v1/schedule/requests/**",
                
                                "/api/v1/study/search",
                                "/api/v1/study/detail",
                                "/api/v1/study/search/**",
                                "/api/v1/study",
                                "/api/v1/study/{studyId}/meetings",
                                "/api/v1/study/meeting/detail",
                                "/api/v1/study/meeting/all",
                                "/api/v1/study/participant/{studyId}/participants/{participantId}",
                                "/api/v1/study/participant/members/{memberId}/participants",
                                "/api/v1/study/participant/{studyId}/participants/pending",
                                "/api/v1/study/participant/{studyId}/participants/approved",

                                // Swagger/OpenAPI 관련 경로 (더 포괄적으로 수정)
                                "/swagger-ui/**",           // 모든 swagger-ui 하위 경로
                                "/swagger-ui.html",
                                "/v3/api-docs/**",          // 모든 api-docs 하위 경로
                                "/swagger-resources/**",    // Swagger 리소스
                                "/webjars/**",             // Swagger UI 웹 자원
                                "/configuration/ui",        // Swagger UI 설정
                                "/configuration/security",  // Swagger 보안 설정
                                "/v3/api-docs",
                                "/actuator/health",
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