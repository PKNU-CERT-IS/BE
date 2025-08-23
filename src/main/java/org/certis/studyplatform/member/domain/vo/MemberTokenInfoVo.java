package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberRole;

public record MemberTokenInfoVo(
        Long memberId,
        String name,           // 회원 이름
        String studentNumber,  // 학번 (JWT username으로 사용)
        String email,          // 이메일 (member_contact 테이블에서)
        MemberRole role        // 회원 역할
) {

    /**
     * 정적 팩토리 메서드
     */
    public static MemberTokenInfoVo of(Long memberId, String name, String studentNumber,
                                       String email, MemberRole role) {
        return new MemberTokenInfoVo(memberId, name, studentNumber, email, role);
    }

    /**
     * 이메일 존재 여부 확인
     */
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

}
