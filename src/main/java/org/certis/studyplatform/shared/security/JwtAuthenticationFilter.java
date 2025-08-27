package org.certis.studyplatform.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
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

                    setAuthenticationUserToContext(accessToken);
                    filterChain.doFilter(request,response);
                    return;
                }
            }
        }
        catch (Exception e){
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_FILTER_PROCESSING_ERROR);
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
        final List<String> excludedPaths = Arrays.asList(
                // 인증 관련
                "/api/v1/member/**",
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
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
