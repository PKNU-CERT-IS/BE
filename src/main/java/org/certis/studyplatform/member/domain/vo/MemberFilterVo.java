package org.certis.studyplatform.member.domain.vo;

/**
 * 회원 필터링 Value Object
 *
 * 회원 목록 조회 시 필터링 조건을 담는 불변 객체
 * Infrastructure Layer에서 JPA 쿼리 조건으로 변환됨
 */
public record MemberFilterVo(
        GradeVo grade,          // 학년 필터 (선택적)
        RoleVo role,            // 역할 필터 (선택적)
        MajorVo major,          // 전공 필터 (선택적)
        Boolean isActive        // 활성 상태 필터 (선택적, null이면 전체)
) {

    /**
     * 빈 필터 생성 (모든 조건 null)
     */
    public static MemberFilterVo empty() {
        return new MemberFilterVo(null, null, null, null);
    }

    /**
     * 활성 회원만 필터링
     */
    public static MemberFilterVo activeOnly() {
        return new MemberFilterVo(null, null, null, true);
    }

    /**
     * 학년별 필터링
     */
    public static MemberFilterVo byGrade(GradeVo grade) {
        return new MemberFilterVo(grade, null, null, true);
    }

    /**
     * 역할별 필터링
     */
    public static MemberFilterVo byRole(RoleVo role) {
        return new MemberFilterVo(null, role, null, true);
    }

    /**
     * 전공별 필터링
     */
    public static MemberFilterVo byMajor(MajorVo major) {
        return new MemberFilterVo(null, null, major, true);
    }

    /**
     * 필터 조건이 있는지 확인
     */
    public boolean hasFilter() {
        return grade != null || role != null || major != null || isActive != null;
    }

    /**
     * 활성 상태 필터가 설정되어 있는지 확인
     */
    public boolean hasActiveFilter() {
        return isActive != null;
    }
}
