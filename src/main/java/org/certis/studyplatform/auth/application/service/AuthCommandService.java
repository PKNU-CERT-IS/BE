package org.certis.studyplatform.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.AuthToken;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.auth.infrastructure.security.JwtTokenProvider;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

   // 로그인 - accessToken,refreshToken 생성
    public AuthToken executeLogin(Auth auth) {
        // accessToken 생성
        AccessTokenVo accessToken = jwtTokenProvider.generateAccessToken(
                auth.getMemberId(),
                auth.getRoleVo().role()
        );

        // refreshToken 생성
        RefreshTokenVo refreshToken = jwtTokenProvider.generateRefreshToken(auth.getMemberId());

        // AuthToken 도메인 객체 생성
        AuthToken authToken = AuthToken.createAuthTokenData(
                auth.getMemberId(),
                auth.getRoleVo().role(),
                accessToken,
                refreshToken
        );

        // Redis에 refreshToken 저장
        refreshTokenRepository.save(refreshToken);

        log.info("로그인 성공: memberId={}, role={}", auth.getMemberId(), auth.getRoleVo().role());

        return authToken;
    }

    //  로그아웃 - RefreshToken 삭제
    public void executeLogout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
        log.info("로그아웃 완료: memberId={}", memberId);
    }

    // accessToken 갱신
    public AccessTokenVo refreshAccessToken(RefreshTokenVo refreshToken ,MemberRole role) {

        // 새 AccessToken 생성
        AccessTokenVo newAccessToken = jwtTokenProvider.generateAccessToken(
                refreshToken.memberId(),
                role
        );

        log.info("AccessToken 갱신 완료: memberId={}", refreshToken.memberId());
        return newAccessToken;
    }

    // 만료된 토큰으로 부터 role 추출하여 리프레시 로직에 활용
    // 이유 1. 리프레시 에는 role 정보를 두지 않음
    // 이유 2. role 정보를 위해 관계형 db에 접근하지 않기 위함
    public MemberRole extractRoleFromExpiredToken(String expiredToken) {
        return jwtTokenProvider.getRoleFromAccessToken(expiredToken);
    }
}
