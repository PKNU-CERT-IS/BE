package org.certis.studyplatform.auth.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Objects;

public record RefreshTokenVo(String value, LocalDateTime expiredAt, Long memberId) {

    // 토큰 값, 만료 날짜 검증 ( NullPointerException 처리 )
    public RefreshTokenVo{
        Objects.requireNonNull(value,"토큰 값은 필수입니다.");
        Objects.requireNonNull(expiredAt,"만료시간은 필수입니다.");
        Objects.requireNonNull(memberId,"회원 ID는 필수입니다.");
    }

    // 토큰 만료 여부 확인
    @JsonIgnore // Jackson은 isXxx() 메서드를 boolean 프로퍼티로 자동 인식합니다: 따라서 레디스 직렬화에 제거하기위한 에노테이션을 붙여줍니다.
    public boolean isExpiredRefreshToken(){
        return LocalDateTime.now().isAfter(expiredAt);
    }

    // 특정 회원의 토큰인지 확인
    public boolean belongsToMember(Long memberId){
        return Objects.equals(memberId,this.memberId);
    }

    public static RefreshTokenVo of(String value, Long memberId, LocalDateTime expiredAt) {
        return new RefreshTokenVo(value, expiredAt, memberId);
    }
}
