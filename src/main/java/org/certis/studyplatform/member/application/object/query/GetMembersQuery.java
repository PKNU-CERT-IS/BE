package org.certis.studyplatform.member.application.object.query;

import org.springframework.data.domain.Pageable;

import java.util.List;

public record GetMembersQuery(
        String name,
        String grade,
        String role,
        String major,
        List<String> skills,
        Pageable pageable
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String grade;
        private String role;
        private String major;
        private List<String> skills;
        private Pageable pageable;

        public Builder name(String name) {
            this.name = name;
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

        public Builder major(String major) {
            this.major = major;
            return this;
        }

        public Builder skills(List<String> skills) {
            this.skills = skills;
            return this;
        }

        public Builder pageable(Pageable pageable) {
            this.pageable = pageable;
            return this;
        }

        public GetMembersQuery build() {
            return new GetMembersQuery(name, grade, role, major, skills, pageable);
        }
    }
}
