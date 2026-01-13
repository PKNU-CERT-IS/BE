package org.certis.studyplatform.shared.config;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.shared.security.JwtAuthenticationFilter;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
import org.certis.studyplatform.shared.security.RateLimitingFilter;
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
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT 필터 인스턴스 생성
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);

        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF 비활성화 (JWT 사용시 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용하지 않음 (JWT는 Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 경로별 권한 설정
                .authorizeHttpRequests(auths -> auths
                        // 잘못된 API 경로들 (커스텀 matcher 사용) - 먼저 처리
                        .requestMatchers(new InvalidApiPathMatcher()).permitAll()
                        // 인증이 필요하지 않은 경로
                        .requestMatchers(
                                "/api/v1/member/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/register",
                                "/api/v1/auth/token/refresh",
                                "/api/v1/blog",
                                "/api/v1/blog/detail",
                                "/api/v1/blog/search",
                                "/api/v1/blog/search/keyword",
                                
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
                                "/api/v1/project/participant/{projectId}/participants",
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
                                "/api/v1/study/participant/{studyId}/participants",
                                "/api/v1/study/participant/members/{memberId}/participants",
                                "/api/v1/study/participant/{studyId}/participants/pending",
                                "/api/v1/study/participant/{studyId}/participants/approved",

                                // Swagger/OpenAPI 관련 경로
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/v3/api-docs",
                                "/actuator/health",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        // Admin API는 STAFF 이상 권한 필요
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("STAFF", "VICECHAIRMAN", "CHAIRMAN", "ADMIN")
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 필터 등록: Rate Limiting 필터를 JWT 필터 앞에 추가
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 기본 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Profile("!test")
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}