package org.certis.studyplatform.auth.domain.model;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.LocalDateTime;

@Builder
@Getter
public class Auth {

    private final Long userId;
    private final MemberRole role;
    private final AccessTokenVo accessToken;
    private final RefreshTokenVo refreshToken;
    private final LocalDateTime createdAt;

    // 새로운 인증 정보 생성 (로그인 시)
    public static Auth createAuthData(Long userId,MemberRole role,
                                      AccessTokenVo accessToken, RefreshTokenVo refreshToken){
        return Auth.builder()
                .userId(userId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 액세스 토큰만 갱신 (리프레시)
    public Auth renewAccessToken(AccessTokenVo accessToken, MemberRole role){
        return Auth.builder()
                .userId(this.userId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(this.refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 두 토큰 모두 갱신 (두 토큰 모두 갱신이 필요한 경우)
    public Auth renewBothTokens(AccessTokenVo accessToken,RefreshTokenVo refreshToken ,MemberRole role){
        return Auth.builder()
                .userId(this.userId)
                .role(role)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 토큰 갱신 가능 여부
    public boolean canRefreshToken(){
        return !refreshToken.isExpiredRefreshToken();
    }

    // 액세스 토큰 유효성 확인
    public boolean isValidAccessToken(){
        return !accessToken.isExpiredAccessToken();
    }
}
