package org.certis.studyplatform.auth.presentation;

import jakarta.security.auth.message.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.service.AuthFacadeService;
import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;


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
    public ResponseEntity<GlobalResponseHandler<LoginResponseDto>> login(
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
        return GlobalResponseHandler.success(ResponseStatus.AUTH_LOGIN_SUCCESS,loginResponse);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponseHandler<Void>> logout(
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletResponse response) {

        log.info("로그아웃 요청: memberId={}", currentUser.getId());

        // 로그아웃 처리
        authFacadeService.logout(currentUser.getId());

        // RefreshToken 쿠키 삭제
        clearRefreshTokenCookie(response);

        log.info("로그아웃 성공: memberId={}", currentUser.getId());
        return GlobalResponseHandler.success(ResponseStatus.AUTH_LOGOUT_SUCCESS);
    }

    /**
     * AccessToken 갱신
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<GlobalResponseHandler<RefreshAccessTokenResponseDto>> refreshToken(
            HttpServletRequest request
    ) {
        log.info("토큰 갱신 요청 시작");

        String refreshToken = extractRefreshTokenFromCookies(request.getCookies());
        RefreshAccessTokenResponseDto responseDto = authFacadeService.refreshAccessToken(refreshToken);

        log.info("토큰 갱신 성공");
        return GlobalResponseHandler.success(ResponseStatus.AUTH_TOKEN_REFRESH_SUCCESS,responseDto);
    }


    @PostMapping("/register")
    public ResponseEntity<GlobalResponseHandler<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("회원가입 요청: accountNumber={}", request.getAccountNumber());

        // 회원가입 처리
        authFacadeService.register(request);

        return GlobalResponseHandler.success(ResponseStatus.AUTH_REGISTER_REQUEST_SUCCESS);
    }

    // 만료된 토큰으로 부터 role 추출하여 리프레시 로직에 활용
    // 이유 1. 리프레시 에는 role 정보를 두지 않음
    // 이유 2. role 정보를 위해 관계형 db에 접근하지 않기 위함
    private MemberRole extractRoleFromAccessToken(String expiredToken) {
        return jwtTokenProvider.getRoleFromAccessToken(expiredToken);
    }

    private String extractUserNameFromAccessToken(String expiredToken) {
        return jwtTokenProvider.getUsernameFromToken(expiredToken);
    }

    private String extractNameFromAccessToken(String expiredToken) {
        return jwtTokenProvider.getNameFromToken(expiredToken);
    }

    private String extractEmailFromAccessToken(String expiredToken) {
        return jwtTokenProvider.getEmailFromToken(expiredToken);
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


    // 쿠키에서 refreshToken 추출
    private String extractRefreshTokenFromCookies(Cookie[] cookies) {
        return Optional.ofNullable(cookies).stream().flatMap(Arrays::stream)
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new PresentationException(ExceptionStatus.AUTH_REFRESH_TOKEN_NOT_FOUND));
    }
}