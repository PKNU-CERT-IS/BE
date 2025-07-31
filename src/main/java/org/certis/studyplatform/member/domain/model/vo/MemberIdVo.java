package org.certis.studyplatform.member.domain.model.vo;

public record MemberIdVo(Long value) {
    public MemberIdVo {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다");
        }
    }
}
