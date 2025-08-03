package org.certis.studyplatform.member.domain.model.vo;

public record EmailVo(String value) {
    public EmailVo {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다");
        }
    }
}