package org.certis.studyplatform.auth.domain.model;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.LocalDateTime;

@Builder
@Getter
public class AuthToken {

    private final Long memberId;
    private final MemberRole role;
    private final AccessTokenVo accessToken;
    private final RefreshTokenVo refreshToken;
    private final LocalDateTime createdAt;

    // 새로운 인증 정보 생성 (로그인 시)
    public static AuthToken createAuthTokenData(Long memberId, MemberRole role,
                                           AccessTokenVo accessToken, RefreshTokenVo refreshToken){
        return AuthToken.builder()
                .memberId(memberId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 액세스 토큰만 갱신 (리프레시)
    public AuthToken renewAccessToken(AccessTokenVo accessToken, MemberRole role){
        return AuthToken.builder()
                .memberId(this.memberId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(this.refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 두 토큰 모두 갱신 (두 토큰 모두 갱신이 필요한 경우)
    public AuthToken renewBothTokens(AccessTokenVo accessToken, RefreshTokenVo refreshToken , MemberRole role){
        return AuthToken.builder()
                .memberId(this.memberId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
