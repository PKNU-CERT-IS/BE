package org.certis.studyplatform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 테스트 환경에서만 사용되는 Mock 인증 필터
 * JWT 토큰 없이도 기본 사용자로 인증 처리
 */
@Slf4j
@Component
@Profile("test") // 테스트 환경에서만 활성화
public class TestAuthenticationFilter extends OncePerRequestFilter {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USERNAME = "testuser";
    private static final String DEFAULT_EMAIL = "test@certis.org";
    private static final String DEFAULT_NAME = "테스트사용자";
    private static final MemberRole DEFAULT_ROLE = MemberRole.PLAYER;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 인증이 필요없는 경로는 건너뛰기
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 이미 인증된 경우 건너뛰기
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Mock 사용자 정보로 인증 처리
            setMockAuthenticationToContext();
            log.debug("테스트 Mock 인증 설정 완료: userId={}, username={}",
                    DEFAULT_USER_ID, DEFAULT_USERNAME);
        } catch (Exception e) {
            log.error("테스트 Mock 인증 설정 실패: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Mock 사용자 인증 정보를 SecurityContext에 설정
     */
    private void setMockAuthenticationToContext() {
        CurrentUser currentUser = new CurrentUser(
                DEFAULT_USER_ID,
                DEFAULT_USERNAME,
                DEFAULT_EMAIL,
                DEFAULT_NAME,
                DEFAULT_ROLE.name()
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        currentUser.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    /**
     * 인증이 필요없는 경로 확인
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();

        List<String> publicPaths = List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/swagger-ui",
                "/v3/api-docs",
                "/actuator/health",
                "/favicon.ico",
                "/error"
        );

        return publicPaths.stream().anyMatch(path::startsWith);
    }
}