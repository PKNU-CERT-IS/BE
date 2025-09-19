package org.certis.studyplatform.member.application.object.command;

import org.certis.studyplatform.member.domain.MemberGrade;

import java.time.OffsetDateTime;
import java.util.List;

public record UpdateProfileCommand(
        Long memberId,
        String name,
        String description,
        String profileImage,
        String major,
        OffsetDateTime birthday,
        String phoneNumber,
        String studentNumber,
        List<String> skills,
        MemberGrade grade,
        String email,
        String githubUrl,
        String linkedinUrl
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long memberId;
        private String name;
        private String description;
        private String profileImage;
        private String major;
        private OffsetDateTime birthday;
        private String phoneNumber;
        private String studentNumber;
        private List<String> skills;
        private MemberGrade grade;
        private String email;
        private String githubUrl;
        private String linkedinUrl;

        public Builder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder profileImage(String profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public Builder major(String major) {
            this.major = major;
            return this;
        }

        public Builder birthday(OffsetDateTime birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder studentNumber(String studentNumber) {
            this.studentNumber = studentNumber;
            return this;
        }

        public Builder skills(java.util.List<String> skills) {
            this.skills = skills;
            return this;
        }

        public Builder grade(MemberGrade grade) {
            this.grade = grade;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder githubUrl(String githubUrl) {
            this.githubUrl = githubUrl;
            return this;
        }

        public Builder linkedinUrl(String linkedinUrl) {
            this.linkedinUrl = linkedinUrl;
            return this;
        }

        public UpdateProfileCommand build() {
            return new UpdateProfileCommand(memberId, name, description, profileImage,
                    major, birthday, phoneNumber, studentNumber, skills, grade, email, githubUrl, linkedinUrl);
        }
    }
}