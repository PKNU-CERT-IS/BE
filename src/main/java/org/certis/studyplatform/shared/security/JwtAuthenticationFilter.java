package org.certis.studyplatform.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            // 인증 관련
            "/api/v1/member/**",
            "/api/v1/auth/login",
            "/api/v1/auth/register",  // 회원가입 추가
            "/api/v1/auth/token/refresh",   // 토큰 갱신
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
            "/api/v1/project/participant/{projectId}/participants/pending/**",
            "/api/v1/project/participant/{projectId}/participants/approved/**",

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
            "/api/v1/study/participant/{studyId}/participants/pending/**",
            "/api/v1/study/participant/{studyId}/participants/approved/**",

            // Swagger/OpenAPI 관련 경로 (더 포괄적으로 수정)
            "/swagger-ui/**",           // 모든 swagger-ui 하위 경로
            "/swagger-ui.html",
            "/v3/api-docs/**",          // 모든 api-docs 하위 경로
            "/swagger-resources/**",    // Swagger 리소스
            "/webjars/**",             // Swagger UI 웹 자원
            "/configuration/ui",        // Swagger UI 설정
            "/configuration/security",  // Swagger 보안 설정
            "/actuator/health",
            "/favicon.ico",
            "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 특정 경로는 인증 필터에서 제외
        if(shouldSkipFilter(request)){
           filterChain.doFilter(request,response);
           return;
        }

        try{
            // HTTP 헤더로부터 JWT 토큰 추출
            String accessToken = resolveToken(request);
            if(StringUtils.hasText(accessToken)&& jwtTokenProvider.isValidateToken(accessToken)){
                if(jwtTokenProvider.isAccessToken(accessToken)){

                    setAuthenticationUserToContext(accessToken);
                    filterChain.doFilter(request,response);
                    return;
                }
            }
        }
        catch (Exception e){
            log.warn("JWT 인증 실패: {}", e.getMessage());
            // JWT 인증 실패 시 401 응답을 위해 AuthenticationException을 던짐
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다\"}");
            return;
        }
        filterChain.doFilter(request,response);
    }

    // Bearer Token 추출
    private String resolveToken(HttpServletRequest request){
        String authorizationHeader = request.getHeader("Authorization");

        if(StringUtils.hasText(authorizationHeader) &&
                authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();

        }

        return null;
    }

    // Jwt 토큰 정보로 인증 처리
    private void setAuthenticationUserToContext(String accessToken){
        try{
            // 토큰에서 사용자 정보 추출
            Long memberId = jwtTokenProvider.getUserIdFromToken(accessToken);
            MemberRole role = jwtTokenProvider.getRoleFromAccessToken(accessToken);
            String username = jwtTokenProvider.getUsernameFromToken(accessToken);
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            String name = jwtTokenProvider.getNameFromToken(accessToken);

            CurrentUser currentUser = new CurrentUser(
                    memberId,
                    username,    // 학번
                    email,       // 이메일
                    name,        // 이름
                    role.name()  // 역할
            );

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            currentUser,
                            null,
                            currentUser.getAuthorities() // ROLE_ 정보 저장
                    );
            // spring security 에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.debug("JWT 인증 성공: memberId={}, username={}, name={}, role={}",
                    memberId, username, name, role);

        } catch (Exception e){
            log.warn("JWT 토큰에서 인증 정보 추출 실패: {}", e.getMessage());
        }
    }

    private boolean shouldSkipFilter(HttpServletRequest request){
        String requestURI = request.getRequestURI();
        
        // 잘못된 API 경로 체크 (커스텀 로직)
        if (isInvalidApiPath(requestURI)) {
            return true; // 잘못된 경로는 필터를 건너뛰어 404 처리되도록 함
        }

        return EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }
    
    /**
     * 잘못된 API 경로인지 확인
     * /api/로 시작하지만 /api/v1/이 아닌 모든 경로를 잘못된 경로로 간주
     */
    private boolean isInvalidApiPath(String requestURI) {
        // /api/로 시작하는지 확인
        if (!requestURI.startsWith("/api/")) {
            return false;
        }
        
        // /api/v1/로 시작하는 경우는 유효한 API
        if (requestURI.startsWith("/api/v1/")) {
            return false;
        }
        
        // /api/로 시작하지만 /api/v1/이 아닌 모든 경로는 잘못된 경로
        return true;
    }
}
