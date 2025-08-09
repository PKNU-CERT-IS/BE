package org.certis.studyplatform.member.domain.vo;

/**
 * 회원 검색 조건용 VO
 */
public record MemberSearchCriteriaVo(
        String keyword,     // 이름, 전공, 기술 스택에서 검색
        String grade,       // 학년 필터 (정확 일치)
        String role,        // 역할 필터 (정확 일치)
        String skill        // 특정 기술 필터 (포함 검색)
) {

    /**
     * 빌더 패턴
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String keyword;
        private String grade;
        private String role;
        private String skill;

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder grade(String grade) {
            this.grade = grade;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder skill(String skill) {
            this.skill = skill;
            return this;
        }

        public MemberSearchCriteriaVo build() {
            return new MemberSearchCriteriaVo(keyword, grade, role, skill);
        }
    }

    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
                (grade == null || grade.trim().isEmpty()) &&
                (role == null || role.trim().isEmpty()) &&
                (skill == null || skill.trim().isEmpty());
    }

    /**
     * 안전한 값 반환 (null과 공백 처리)
     */
    public String getSafeKeyword() {
        return keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null;
    }

    public String getSafeGrade() {
        return grade != null && !grade.trim().isEmpty() ? grade.trim() : null;
    }

    public String getSafeRole() {
        return role != null && !role.trim().isEmpty() ? role.trim() : null;
    }

    public String getSafeSkill() {
        return skill != null && !skill.trim().isEmpty() ? skill.trim() : null;
    }
}