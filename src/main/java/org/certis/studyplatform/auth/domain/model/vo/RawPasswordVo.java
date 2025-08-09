package org.certis.studyplatform.auth.domain.model.vo;

public record RawPasswordVo(String value) {
    public static RawPasswordVo of(String value) {
        return new RawPasswordVo(value);
    }
}
