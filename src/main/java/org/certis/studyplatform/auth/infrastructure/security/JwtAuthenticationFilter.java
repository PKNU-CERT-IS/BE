package org.certis.studyplatform.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

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
                    authenticationUser(accessToken);
                    filterChain.doFilter(request,response);
                    return;
                }else{
                    log.warn("유효하지 않은 AccessToken");
                    // 예외처리 이후
                    return;
                }
            }else{
                log.warn("AccessToken 만료 또는 유효하지 않음");
                //예외처리 이후
            }
        }
        catch (Exception e){
            // 예외 만들기 이후
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                // 예외 만들기 이후
            }
        }
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
    private void authenticationUser(String accessToken){
        try{
            Long memberId = jwtTokenProvider.getUserIdFromToken(accessToken);
            MemberRole role = jwtTokenProvider.getRoleFromAccessToken(accessToken);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(role.toAuthorityString())) // ROLE_ 정보 저장
                    );
            // spring security 에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.debug("JWT 인증 성공: memberId={}, role={}", memberId, role);
        }catch (Exception e){
            log.warn("JWT 토큰에서 인증 정보 추출 실패: {}", e.getMessage());
        }
    }

    private boolean shouldSkipFilter(HttpServletRequest request){
        final List<String> excludedPaths = Arrays.asList(
                // 인증 관련
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/api/v1/auth/token/refresh",

                //문서 모니터링
                "/swagger-ui.html",
                "/swagger-ui",
                "/v3/api-docs"
        );

        String path = request.getRequestURI();

        return excludedPaths.stream()
                .anyMatch(path::startsWith);
    }
}
