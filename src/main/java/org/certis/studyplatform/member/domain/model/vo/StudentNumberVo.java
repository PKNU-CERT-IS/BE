package org.certis.studyplatform.member.domain.model.vo;

public record StudentNumberVo(String value) {
    public StudentNumberVo {
        if (value == null || !value.matches("\\d{8,10}")) {
            throw new IllegalArgumentException("학번은 8-10자리 숫자여야 합니다");
        }
    }
}