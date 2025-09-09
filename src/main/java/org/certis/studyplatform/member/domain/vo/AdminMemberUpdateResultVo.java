package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

public record AdminMemberUpdateResultVo(
        Long memberId,
        MemberRole newRole,
        MemberGrade newGrade
) {
    public AdminMemberUpdateResultVo {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("현재 권한은 필수입니다");
        }
        if (newGrade == null) {
            throw new IllegalArgumentException("현재 학년은 필수입니다");
        }
    }

    public static AdminMemberUpdateResultVo of(Long memberId, MemberRole newRole, MemberGrade newGrade) {
        return new AdminMemberUpdateResultVo(memberId, newRole, newGrade);
    }
}
