package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.security.crypto.password.PasswordEncoder;

public record AuthInfoVo(Long memberId, String accountNumber, String encodedPassword, MemberRole role) {
    public static AuthInfoVo of(Long memberId, String accountNumber, String encodedPassword, MemberRole role) {
        return new AuthInfoVo(memberId, accountNumber, encodedPassword, role);
    }

    // 비밀번호 검증 비즈니스 로직
    public boolean isPasswordMatches(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.encodedPassword);
    }
}
