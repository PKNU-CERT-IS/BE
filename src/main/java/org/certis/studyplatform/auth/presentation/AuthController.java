package org.certis.studyplatform.auth.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.service.AuthFacadeService;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
import org.certis.studyplatform.auth.presentation.dto.request.LoginRequestDto;
import org.certis.studyplatform.auth.presentation.dto.response.RefreshAccessTokenResponseDto;
import org.certis.studyplatform.auth.presentation.dto.response.LoginResponseDto;
import org.certis.studyplatform.auth.presentation.dto.response.TokenRequestDto;
import org.certis.studyplatform.exception.PresentationException;
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
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
           @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {

        log.info("로그인 요청: accountNumber={}", request.getAccountNumber());

        // 로그인 처리
        TokenRequestDto tokenRequestDto = authFacadeService.login(request);

        // RefreshToken을 HttpOnly 쿠키로 설정 (실제 만료시간 사용)
        setRefreshTokenCookie(response, tokenRequestDto);

        // AccessToken만 응답 Body에
        LoginResponseDto loginResponse = new LoginResponseDto(
                tokenRequestDto.getAccessToken(),
                tokenRequestDto.getMemberId(),
                tokenRequestDto.getRole()
        );

        log.info("로그인 성공: memberId={}", loginResponse.getMemberId());
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
    public ResponseEntity<RefreshAccessTokenResponseDto> refreshToken(
            HttpServletRequest request,
            @AuthenticationPrincipal Long memberId) {

        log.info("토큰 갱신 요청: memberId={}", memberId);

        // 만료된 AccessToken 에서 role 추출 (Command Service를 통해)
        String accessToken = extractTokenFromHeader(request);
        MemberRole currentRole = extractRoleFromAccessToken(accessToken);

        // 토큰 갱신 ( memberId는 검증된 값 currentRole 도 또한 검증된 값 따라서 dto 감싸는건 과다하다고 생각)
        RefreshAccessTokenResponseDto responseDto = authFacadeService.refreshAccessToken(memberId, currentRole);

        log.info("토큰 갱신 성공: memberId={}", memberId);
        return ResponseEntity.ok(responseDto);
    }

    // 만료된 토큰으로 부터 role 추출하여 리프레시 로직에 활용
    // 이유 1. 리프레시 에는 role 정보를 두지 않음
    // 이유 2. role 정보를 위해 관계형 db에 접근하지 않기 위함
    private MemberRole extractRoleFromAccessToken(String expiredToken) {
        return jwtTokenProvider.getRoleFromAccessToken(expiredToken);
    }

    /**
     * RefreshToken 쿠키 설정 (실제 토큰 만료시간 사용)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, TokenRequestDto tokenRequestDto) {
        Cookie cookie = new Cookie("refreshToken", tokenRequestDto.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTP 에서도 동작 이후 true로 바꿔야함
        cookie.setPath("/");

        // RefreshToken의 실제 만료시간으로 쿠키 만료시간 설정
        long ttlSeconds = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                tokenRequestDto.getRefreshExpiredAt()
        ).getSeconds();
        cookie.setMaxAge((int) ttlSeconds);

        cookie.setAttribute("SameSite", "Strict"); // CSRF 방지
        response.addCookie(cookie);
    }

    // refresh 토큰 삭제
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
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
            throw new PresentationException(ExceptionStatus.AUTH_PRESENTATION_INVALID_REQUEST);
        }
        return authHeader.substring(7).trim();
    }
}