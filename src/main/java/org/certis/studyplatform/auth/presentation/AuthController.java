package org.certis.studyplatform.auth.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.service.AuthCommandService;
import org.certis.studyplatform.auth.application.service.AuthFacadeService;
import org.certis.studyplatform.auth.domain.model.AuthToken;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.infrastructure.security.JwtTokenProvider;
import org.certis.studyplatform.auth.presentation.dto.request.LoginRequestDto;
import org.certis.studyplatform.auth.presentation.dto.response.AccessTokenRefreshResponseDto;
import org.certis.studyplatform.auth.presentation.dto.response.LoginResponseDto;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacadeService authFacadeService;
    private final AuthCommandService authCommandService;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto request,
            HttpServletResponse response) {

        log.info("로그인 요청: accountNumber={}", request.getAccountNumber());

        // 로그인 처리
        AuthToken authToken = authFacadeService.login(request.getAccountNumber(), request.getPassword());

        // RefreshToken을 HttpOnly 쿠키로 설정 (실제 만료시간 사용)
        setRefreshTokenCookie(response, authToken.getRefreshToken());

        // AccessToken만 응답 Body에
        LoginResponseDto loginResponse = new LoginResponseDto(
                authToken.getAccessToken().value(),
                authToken.getMemberId(),
                authToken.getRole()
        );

        log.info("로그인 성공: memberId={}", authToken.getMemberId());
        return ResponseEntity.ok(loginResponse);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long memberId,
            HttpServletResponse response) {

        log.info("로그아웃 요청: memberId={}", memberId);

        // 로그아웃 처리
        authFacadeService.logout(memberId);

        // RefreshToken 쿠키 삭제
        clearRefreshTokenCookie(response);

        log.info("로그아웃 성공: memberId={}", memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * AccessToken 갱신
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<AccessTokenRefreshResponseDto> refreshToken(
            HttpServletRequest request,
            @AuthenticationPrincipal Long memberId) {

        log.info("토큰 갱신 요청: memberId={}", memberId);

        // 만료된 AccessToken에서 role 추출 (Command Service를 통해)
        String accessToken = extractTokenFromHeader(request);
        MemberRole currentRole = authCommandService.extractRoleFromAccessToken(accessToken);

        // 토큰 갱신
        AccessTokenVo newAccessToken = authFacadeService.refreshAccessToken(memberId, currentRole);

        AccessTokenRefreshResponseDto response = new AccessTokenRefreshResponseDto(
                newAccessToken.value()
        );

        log.info("토큰 갱신 성공: memberId={}", memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * RefreshToken 쿠키 설정 (실제 토큰 만료시간 사용)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, RefreshTokenVo refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken.value());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTP 에서도 동작 이후 true로 바꿔야함
        cookie.setPath("/");

        // RefreshToken의 실제 만료시간으로 쿠키 만료시간 설정
        long ttlSeconds = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                refreshToken.expiredAt()
        ).getSeconds();
        cookie.setMaxAge((int) ttlSeconds);

        cookie.setAttribute("SameSite", "Strict"); // CSRF 방지
        response.addCookie(cookie);
    }

    // refresh 토큰 삭제
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
    }

    // Authorization 헤더에서 토큰 추출
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
        }
        return authHeader.substring(7).trim();
    }
}