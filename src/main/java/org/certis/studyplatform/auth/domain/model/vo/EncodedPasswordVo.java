package org.certis.studyplatform.auth.domain.model.vo;

import java.util.Objects;

public record EncodedPasswordVo (String encodedPassword) {
    public EncodedPasswordVo {
        Objects.requireNonNull(encodedPassword, "암호화된 비밀번호는 필수입니다.");
    }

    public static EncodedPasswordVo of(String encodedPassword) {
        return new EncodedPasswordVo(encodedPassword);
    }
}
