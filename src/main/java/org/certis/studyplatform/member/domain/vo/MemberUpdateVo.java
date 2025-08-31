package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

/**
 * 회원 수정 Value Object
 *
 * 회원 정보 수정 시 사용되는 정보를 담는 불변 객체
 * null 값은 해당 필드를 수정하지 않음을 의미
 */
public record MemberUpdateVo(
        NameVo name,                      // 이름 (선택적)
        StudentNumberVo studentNumber,    // 학번 (일반적으로 수정 불가)
        GradeVo grade,                    // 학년 (선택적)
        RoleVo role,                      // 역할 (선택적)
        MajorVo major,                    // 전공 (선택적)
        SkillsVo skills,                  // 기술 스택 (선택적)
        String description                // 설명 (선택적)
) {

    /**
     * Builder 패턴
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private NameVo name;
        private StudentNumberVo studentNumber;
        private GradeVo grade;
        private RoleVo role;
        private MajorVo major;
        private SkillsVo skills;
        private String description;

        public Builder name(NameVo name) {
            this.name = name;
            return this;
        }

        public Builder studentNumber(StudentNumberVo studentNumber) {
            this.studentNumber = studentNumber;
            return this;
        }

        public Builder grade(GradeVo grade) {
            this.grade = grade;
            return this;
        }

        public Builder role(RoleVo role) {
            this.role = role;
            return this;
        }

        public Builder major(MajorVo major) {
            this.major = major;
            return this;
        }

        public Builder skills(SkillsVo skills) {
            this.skills = skills;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public MemberUpdateVo build() {
            return new MemberUpdateVo(name, studentNumber, grade, role, major, skills, description);
        }
    }

    // =================================================================
    // 업데이트 여부 확인 메서드들
    // =================================================================

    /**
     * 이름 업데이트가 있는지 확인
     */
    public boolean hasNameUpdate() {
        return name != null;
    }

    /**
     * 학번 업데이트가 있는지 확인
     */
    public boolean hasStudentNumberUpdate() {
        return studentNumber != null;
    }

    /**
     * 학년 업데이트가 있는지 확인
     */
    public boolean hasGradeUpdate() {
        return grade != null;
    }

    /**
     * 역할 업데이트가 있는지 확인
     */
    public boolean hasRoleUpdate() {
        return role != null;
    }

    /**
     * 전공 업데이트가 있는지 확인
     */
    public boolean hasMajorUpdate() {
        return major != null;
    }

    /**
     * 기술 스택 업데이트가 있는지 확인
     */
    public boolean hasSkillsUpdate() {
        return skills != null;
    }

    /**
     * 설명 업데이트가 있는지 확인
     */
    public boolean hasDescriptionUpdate() {
        return description != null;
    }

    /**
     * 어떤 업데이트라도 있는지 확인
     */
    public boolean hasAnyUpdate() {
        return hasNameUpdate() || hasStudentNumberUpdate() || hasGradeUpdate() ||
               hasRoleUpdate() || hasMajorUpdate() || hasSkillsUpdate() || hasDescriptionUpdate();
    }

    // =================================================================
    // Getter 메서드들 (Infrastructure Layer에서 사용)
    // =================================================================

    /**
     * 이름 문자열 반환 (nullable)
     */
    public String getNameValue() {
        return name != null ? name.value() : null;
    }

    /**
     * 학번 문자열 반환 (nullable)
     */
    public String getStudentNumberValue() {
        return studentNumber != null ? studentNumber.value() : null;
    }

    /**
     * 학년 문자열 반환 (nullable)
     */
    public String getGradeValue() {
        return grade != null ? grade.value() : null;
    }

    /**
     * 역할 문자열 반환 (nullable)
     */
    public MemberRole getRoleValue() {
        return role != null ? role.role() : null;
    }

    /**
     * 전공 문자열 반환 (nullable)
     */
    public String getMajorValue() {
        return major != null ? major.value() : null;
    }

    /**
     * 기술 스택 목록 반환 (nullable)
     */
    public List<String> getSkillsValues() {
        return skills != null ? skills.values() : null;
    }
}