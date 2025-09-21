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
import org.springframework.security.core.Authentication;
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
        // @WithMockUser에서 설정한 사용자 정보 확인
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        
        if (existingAuth != null && existingAuth.getPrincipal() instanceof CurrentUser) {
            // 이미 @WithMockUser로 설정된 사용자가 있으면 그대로 사용
            log.debug("테스트 Mock 인증 - 기존 사용자 사용: {}", existingAuth.getPrincipal());
            return;
        }
        
        // @WithMockUser에서 설정한 사용자 정보가 있는지 확인
        if (existingAuth != null && existingAuth.getPrincipal() instanceof String) {
            String username = (String) existingAuth.getPrincipal();
            log.debug("테스트 Mock 인증 - @WithMockUser 사용자: {}", username);
            
            // @WithMockUser에서 설정한 역할 정보 가져오기
            String role = getRoleFromAuthorities(existingAuth);
            if (role == null) {
                role = getRoleByUsername(username);
            }
            
            // username에 따라 사용자 ID 매핑
            Long userId = getUserIdByUsername(username);
            String email = getEmailByUsername(username);
            String name = getNameByUsername(username);
            
            CurrentUser currentUser = new CurrentUser(userId, username, email, name, role);
            
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            currentUser,
                            null,
                            currentUser.getAuthorities()
                    );
            
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            return;
        }
        
        // 기본 Mock 사용자 정보로 인증 처리
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
     * username에 따른 사용자 ID 매핑
     */
    private Long getUserIdByUsername(String username) {
        switch (username) {
            case "unauthorized":
                return 999L; // 권한이 없는 사용자
            case "testuser":
                return 1L; // 기본 테스트 사용자
            default:
                return DEFAULT_USER_ID;
        }
    }
    
    /**
     * username에 따른 이메일 매핑
     */
    private String getEmailByUsername(String username) {
        switch (username) {
            case "unauthorized":
                return "unauthorized@certis.org";
            case "testuser":
                return "test@certis.org";
            default:
                return DEFAULT_EMAIL;
        }
    }
    
    /**
     * username에 따른 이름 매핑
     */
    private String getNameByUsername(String username) {
        switch (username) {
            case "unauthorized":
                return "권한없음";
            case "testuser":
                return "테스트사용자";
            default:
                return DEFAULT_NAME;
        }
    }
    
    /**
     * @WithMockUser에서 설정한 권한에서 역할 추출
     */
    private String getRoleFromAuthorities(Authentication auth) {
        if (auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return null;
        }
        
        // 첫 번째 권한에서 역할 추출 (ROLE_ 접두사 제거)
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        if (authority.startsWith("ROLE_")) {
            return authority.substring(5); // "ROLE_" 제거
        }
        return authority;
    }
    
    /**
     * username에 따른 역할 매핑
     */
    private String getRoleByUsername(String username) {
        switch (username) {
            case "unauthorized":
                return "PLAYER";
            case "testuser":
                return "UPSOLVER";
            default:
                return DEFAULT_ROLE.name();
        }
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