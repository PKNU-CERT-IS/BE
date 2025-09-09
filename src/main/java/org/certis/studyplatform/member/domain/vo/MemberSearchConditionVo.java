package org.certis.studyplatform.member.domain.vo;

import lombok.Builder;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

/**
 * 회원 검색 조건 VO
 */
@Builder
public record MemberSearchConditionVo(
        String keyword,
        GradeVo grade,
        RoleVo role
) {
    public static MemberSearchConditionVo of(String searchKeyword, MemberGrade grade, MemberRole role) {
        return new MemberSearchConditionVo(
                searchKeyword,
                grade != null ? GradeVo.of(grade) : null,
                role != null ? RoleVo.of(role) : null
        );
    }
}