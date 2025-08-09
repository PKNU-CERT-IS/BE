package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.member.domain.MemberRole;

import java.time.LocalDateTime;

public record TokenInfoVo(String accessToken,
                          String refreshToken,
                          Long memberId,
                          LocalDateTime refreshExpiredAt,
                          MemberRole role) {
    public static TokenInfoVo of(String accessToken, String refreshToken, Long memberId,
                                 LocalDateTime refreshExpiredAt, MemberRole role) {
        return new TokenInfoVo(accessToken, refreshToken, memberId, refreshExpiredAt, role);
    }
}
