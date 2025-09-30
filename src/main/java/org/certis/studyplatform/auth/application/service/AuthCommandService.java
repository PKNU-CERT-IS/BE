package org.certis.studyplatform.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.object.command.CreateAuthCommand;
import org.certis.studyplatform.auth.application.object.command.GenerateTokenCommand;
import org.certis.studyplatform.auth.application.object.command.LogoutCommand;
import org.certis.studyplatform.auth.application.object.command.RefreshTokenCommand;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.TokenInfoVo;
import org.certis.studyplatform.auth.domain.service.AuthDomainService;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthDomainService authDomainService;

   // 로그인 - accessToken,refreshToken 생성
    public TokenInfoVo executeLogin(GenerateTokenCommand generateTokenCommand) {
        // accessToken 생성
        AccessTokenVo accessToken = jwtTokenProvider.generateAccessToken(
                generateTokenCommand.memberId(),
                generateTokenCommand.username(),
                generateTokenCommand.email(),
                generateTokenCommand.name(),
                generateTokenCommand.role()
        );

        // refreshToken 생성
        RefreshTokenVo refreshToken = jwtTokenProvider.generateRefreshToken(generateTokenCommand.memberId());

        // Redis에 refreshToken 저장
        authDomainService.saveRefreshToken(refreshToken);

        TokenInfoVo tokenInfoVo = TokenInfoVo.of(
                accessToken.value(),
                refreshToken.value(),
                generateTokenCommand.memberId(),
                refreshToken.expiredAt(),
                generateTokenCommand.role()
        );

        log.info("로그인 성공: memberId={}, role={}", generateTokenCommand.memberId(),generateTokenCommand.role());

        return tokenInfoVo;
    }

    //  로그아웃 - RefreshToken 삭제
    public void executeLogout(LogoutCommand logoutCommand) {

        authDomainService.deleteRefreshToken(logoutCommand);
    }

    // accessToken 갱신
    public AccessTokenVo refreshAccessToken(RefreshTokenCommand refreshTokenCommand) {

        // 새 AccessToken 생성
        AccessTokenVo newAccessToken = jwtTokenProvider.generateAccessToken(
                refreshTokenCommand.memberId(),
                refreshTokenCommand.username(),
                refreshTokenCommand.email(),
                refreshTokenCommand.name(),
                refreshTokenCommand.role()
        );

        // 보안 강화: 새로운 RefreshToken도 생성하여 토큰 로테이션 적용
        RefreshTokenVo newRefreshToken = jwtTokenProvider.generateRefreshToken(refreshTokenCommand.memberId());
        authDomainService.saveRefreshToken(newRefreshToken);

        log.info("AccessToken 및 RefreshToken 갱신 완료: memberId={}", refreshTokenCommand.memberId());
        return newAccessToken;
    }

    @Transactional
    public void createAuth(CreateAuthCommand createAuthCommand) {
        log.info("🎯 Auth Command Service: Creating auth for memberId: {}, accountNumber: {}",
                createAuthCommand.memberId(), createAuthCommand.accountNumber());

        // Domain Service 호출 (비즈니스 로직 위임)
        authDomainService.createAuth(createAuthCommand);

        log.info("✅ Auth Command Service: Auth created successfully for memberId: {}",
                createAuthCommand.memberId());
    }

}
