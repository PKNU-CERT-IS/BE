package org.certis.studyplatform.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.AuthToken;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final AuthQueryService authQueryService;
    private final AuthCommandService authCommandService;

   // 로그인
    @Transactional
    public AuthToken login(String accountNumber, String rawPassword) {
        log.info("로그인 시도: accountNumber={}", accountNumber);

        // 1. 자격증명 검증
        Auth auth = authQueryService.validateCredentials(accountNumber, rawPassword);

        // 2. 토큰 생성 및 Redis 저장
        AuthToken authToken = authCommandService.executeLogin(auth);

        log.info("로그인 성공: memberId={}", auth.getMemberId());
        return authToken;
    }

    // 로그아웃
    @Transactional
    public void logout(Long memberId) {
        log.info("로그아웃 시도: memberId={}", memberId);

        // 레디스에서 토큰 정보 삭제
        authCommandService.executeLogout(memberId);

        log.info("로그아웃 성공: memberId={}", memberId);
    }

   // accessToken 갱신
    @Transactional
    public AccessTokenVo refreshAccessToken(Long memberId, MemberRole currentRole) {
        log.info("토큰 갱신 시도: memberId={}", memberId);

        // 1. RefreshToken 검증
        RefreshTokenVo refreshToken = authQueryService.validateRefreshToken(memberId);

        // 2. 새 AccessToken 생성
        AccessTokenVo newAccessToken = authCommandService.refreshAccessToken(refreshToken, currentRole);

        log.info("토큰 갱신 성공: memberId={}", memberId);
        return newAccessToken;
    }
}