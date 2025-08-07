package org.certis.studyplatform.auth.domain.service;


import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.AuthToken;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthDomainService {

    // 인증 정보 생성
    public AuthToken createAuthData(Long memberId, MemberRole role,
                                    AccessTokenVo accessToken, RefreshTokenVo refreshToken) {
        log.info("인증 데이터 생성: userId={}, role={}", memberId, role);
        return AuthToken.createAuthTokenData(memberId, role, accessToken, refreshToken);
    }

    // 액세스 토큰 갱신
    public AuthToken renewAccessToken(AuthToken currentAuth, AccessTokenVo newAccessToken, MemberRole role) {
        if (!currentAuth.canRefreshToken()) {
            // 이후 토큰 만료 예외 처리
        }

        log.info("액세스 토큰 갱신: userId={}", currentAuth.getMemberId());
        return currentAuth.renewAccessToken(newAccessToken, role);
    }

    // 토큰 유효성 검증
    public void validateRefreshToken(RefreshTokenVo storedToken, String requestToken, Long memberId) {

        if (!storedToken.belongsToMember(memberId)) {
            throw new IllegalArgumentException("토큰의 소유자가 일치하지 않습니다.");
        }

        if (!storedToken.value().equals(requestToken)) {
            throw new IllegalArgumentException("토큰이 일치하지 않습니다.");
        }

        if (storedToken.isExpiredRefreshToken()) {
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다.");
        }
    }

    // 비밀번호 검증 정책 ( 로그인 시도 제한, 계정 잠금등의 비즈니스 로직 추가 가능 )
    public void validateLoginAttempt(String accountNumber) {
            log.debug("로그인 시도 검증: accountNumber={}", accountNumber);
        }
}
