package org.certis.studyplatform.auth.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccessTokenVo(String value, LocalDateTime expiredAt) {

    // 토큰 값, 만료 날짜 검증 ( NullPointerException 처리 )
    public AccessTokenVo{
        Objects.requireNonNull(value,"토큰 값은 필수입니다.");
        Objects.requireNonNull(expiredAt,"만료시간은 필수입니다.");
    }

}
