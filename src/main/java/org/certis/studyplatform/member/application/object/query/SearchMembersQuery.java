package org.certis.studyplatform.member.application.object.query;

import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * 회원 검색 쿼리
 *
 * 회원 검색 시 사용되는 조건들을 담는 불변 객체
 * 이름, 전공, 기술스택으로 검색 가능
 */
public record SearchMembersQuery(
        String keyword,        // 검색 키워드 (이름, 전공, 기술스택에서 검색)
        String grade,          // 학년 필터 (정확 일치)
        MemberRole role,           // 역할 필터 (정확 일치)
        List<String> skills,   // 기술스택 필터 (포함 검색)
        Pageable pageable      // 페이징 정보
) {

    /**
     * 검색 조건이 있는지 확인
     */
    public boolean hasSearchCriteria() {
        return (keyword != null && !keyword.trim().isEmpty()) ||
               (grade != null && !grade.trim().isEmpty()) ||
               (role != null) ||
               (skills != null && !skills.isEmpty());
    }

    /**
     * 키워드 검색이 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 학년 필터가 있는지 확인
     */
    public boolean hasGradeFilter() {
        return grade != null && !grade.trim().isEmpty();
    }

    /**
     * 역할 필터가 있는지 확인
     */
    public boolean hasRoleFilter() {
        return role != null ;
    }

    /**
     * 기술스택 필터가 있는지 확인
     */
    public boolean hasSkillsFilter() {
        return skills != null && !skills.isEmpty();
    }
}