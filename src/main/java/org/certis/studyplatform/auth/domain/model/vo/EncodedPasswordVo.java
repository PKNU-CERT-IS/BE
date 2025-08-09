package org.certis.studyplatform.auth.domain.model.vo;


public record EncodedPasswordVo (String encodedPassword) {
    public static EncodedPasswordVo of(String encodedPassword) {
        return new EncodedPasswordVo(encodedPassword);
    }
}
