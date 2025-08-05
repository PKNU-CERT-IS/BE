package org.certis.studyplatform.auth.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;

public record RefreshTokenVo(String value, LocalDateTime expiredAt, Long userId) {

    // 토큰 값, 만료 날짜 검증 ( NullPointerException 처리 )
    public RefreshTokenVo{
        Objects.requireNonNull(value,"토큰 값은 필수입니다.");
        Objects.requireNonNull(expiredAt,"만료시간은 필수입니다.");
        Objects.requireNonNull(userId,"회원 ID는 필수입니다.");
    }

    // 토큰 만료 여부 확인
    public boolean isExpiredRefreshToken(){
        return LocalDateTime.now().isAfter(expiredAt);
    }

    // 특정 회원의 토큰인지 확인
    public boolean belongsToMember(Long userId){
        return Objects.equals(userId,this.userId);
    }
}
