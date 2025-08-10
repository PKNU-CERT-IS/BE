package org.certis.studyplatform.member.domain.vo;

import lombok.Builder;

/**
 * 회원 검색 조건 VO
 */
@Builder
public record MemberSearchConditionVo(
        String keyword,
        GradeVo grade,
        RoleVo role,
        MajorVo major,
        SkillsVo skills
) {
    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
                grade == null && role == null && major == null && skills == null;
    }
}