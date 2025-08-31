package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberRole;

import java.util.Collections;
import java.util.List;

/**
 * 회원 생성 Value Object
 *
 * 새로운 회원을 생성할 때 필요한 모든 정보를 담는 불변 객체
 * Domain Layer에서 Infrastructure Layer로 전달되는 복합 VO
 */
public record MemberCreationVo(
        NameVo name,                      // 이름 (필수)
        StudentNumberVo studentNumber,    // 학번 (필수)
        GradeVo grade,                    // 학년 (필수)
        RoleVo role,                      // 역할 (필수)
        MajorVo major,                    // 전공 (필수)
        SkillsVo skills,                  // 기술 스택 (필수)
        String description,               // 설명 (선택적)
        EmailVo email,                    // 이메일 (선택적)
        ProfileImageVo profileImage,      // 프로필 이미지 (선택적)
        BirthdayVo birthday,
        GenderVo gender

) {

    public MemberCreationVo {
        if (name == null) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        if (studentNumber == null) {
            throw new IllegalArgumentException("학번은 필수입니다");
        }
        if (grade == null) {
            throw new IllegalArgumentException("학년은 필수입니다");
        }
        if (role == null) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        if (major == null) {
            throw new IllegalArgumentException("전공은 필수입니다");
        }
//        if (skills == null) {
//            throw new IllegalArgumentException("기술 스택은 필수입니다");
//        }
    }

    /**
     * 팩토리 메서드 (개별 VO들로부터 생성)
     */
    public static MemberCreationVo from(NameVo name, StudentNumberVo studentNumber,
                                        GradeVo grade, RoleVo role, MajorVo major,
                                        SkillsVo skills, String description,
                                        EmailVo email, ProfileImageVo profileImage, BirthdayVo birthday, GenderVo gender) {
        return new MemberCreationVo(name, studentNumber, grade, role, major,
                                   skills, description, email, profileImage, birthday,gender);
    }

    /**
     * 팩토리 메서드 (필수 필드만 -> 이럴때는 of 보다는 메서드이름을 특정지을 수 있게 만드는게 좋다고 하더라구요)
     */
    public static MemberCreationVo of(NameVo name, StudentNumberVo studentNumber,
                                      GradeVo grade, RoleVo role, MajorVo major,
                                      SkillsVo skills,BirthdayVo birthday, GenderVo gender) {
        return new MemberCreationVo(name, studentNumber, grade, role, major,
                                   skills, null, null, null,birthday,gender);
    }


    /**
     * 회원가입 전용 팩토리 메서드
     * skills, description, email, profileImage는 null로 설정
     */
    public static MemberCreationVo forRegistration(NameVo name, StudentNumberVo studentNumber,
                                                   GradeVo grade, RoleVo role, MajorVo major,
                                                   BirthdayVo birthday, GenderVo gender) {
        return new MemberCreationVo(
                name, studentNumber, grade, role, major,
                null,        // skills -> null
                null,        // description -> null
                null,        // email -> null
                null,        // profileImage -> null
                birthday, gender
        );
    }

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
        private EmailVo email;
        private ProfileImageVo profileImage;
        private BirthdayVo birthday;
        private GenderVo gender;

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

        public Builder email(EmailVo email) {
            this.email = email;
            return this;
        }

        public Builder profileImage(ProfileImageVo profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public Builder birthday(BirthdayVo birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder gender(GenderVo gender) {
            this.gender = gender;
            return this;
        }


        public MemberCreationVo build() {
            return new MemberCreationVo(name, studentNumber, grade, role, major,
                                       skills, description, email, profileImage,birthday,gender);
        }
    }

    // =================================================================
    // Getter 메서드들 (Infrastructure Layer에서 사용)
    // =================================================================

    /**
     * 이름 문자열 반환
     */
    public String getNameValue() {
        return name.value();
    }

    /**
     * 학번 문자열 반환
     */
    public String getStudentNumberValue() {
        return studentNumber.value();
    }

    /**
     * 학년 문자열 반환
     */
    public String getGradeValue() {
        return grade.value();
    }

    /**
     * 역할 문자열 반환
     */
    public MemberRole getRoleValue() {
        return role.role();
    }

    /**
     * 전공 문자열 반환
     */
    public String getMajorValue() {
        return major.value();
    }

    /**
     * 기술 스택 목록 반환
     */
    public List<String> getSkillsValues() {
        return  skills != null ? skills.values() : Collections.emptyList();
    }

    /**
     * 이메일 문자열 반환 (nullable)
     */
    public String getEmailValue() {
        return email != null ? email.value() : null;
    }

    /**
     * 프로필 이미지 URL 반환 (nullable)
     */
    public String getProfileImageValue() {
        return profileImage != null ? profileImage.value() : null;
    }

    /**
     * 이메일이 있는지 확인
     */
    public boolean hasEmail() {
        return email != null;
    }

    /**
     * 프로필 이미지가 있는지 확인
     */
    public boolean hasProfileImage() {
        return profileImage != null;
    }

    /**
     * 설명이 있는지 확인
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}