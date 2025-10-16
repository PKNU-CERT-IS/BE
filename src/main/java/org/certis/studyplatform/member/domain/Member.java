package org.certis.studyplatform.member.domain;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.member.domain.vo.*;
import java.time.OffsetDateTime;

@Deprecated(forRemoval = true)
@Builder
@Getter
public class Member {
    private final Long id;
    private NameVo name;
    private final StudentNumberVo studentNumber;
    private ProfileImageVo profileImage;
    private MemberGrade grade;
    private MemberRole role;
    private SkillsVo skills;
    private MajorVo major;
    private String description;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 새 회원 생성
    public Member(String name, String studentNumber, MemberGrade grade,
                  java.util.List<String> skills, MemberRole role, String major) {
        this.id = null;
        this.name = NameVo.of(name);
        this.studentNumber = new StudentNumberVo(studentNumber);
        this.profileImage = null;
        this.grade = grade;
        this.role = role;
        this.skills = new SkillsVo(skills);
        this.major = MajorVo.of(major);
        this.description = null;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // 기존 회원 복원 (도메인 VO 버전)

    public Member(Long id, NameVo name, StudentNumberVo studentNumber,
                  ProfileImageVo profileImage, MemberGrade grade, MemberRole role,
                  SkillsVo skills, MajorVo major, String description,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.studentNumber = studentNumber;
        this.profileImage = profileImage;
        this.grade = grade;
        this.role = role;
        this.skills = skills;
        this.major = major;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 기존 회원 복원 (Primitive 타입 버전 - Repository 계층에서 사용)
    public Member(Long id, String name, StudentNumberVo studentNumber,
                  ProfileImageVo profileImage, MemberGrade grade, MemberRole role,
                  SkillsVo skills, String major,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = NameVo.of(name);
        this.studentNumber = studentNumber;
        this.profileImage = profileImage;
        this.grade = grade;
        this.role = role;
        this.skills = skills;
        this.major = MajorVo.of(major);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory method for domain creation
    public static Member create(String name, String studentNumber, MemberGrade grade,
                              java.util.List<String> skills, MemberRole role, String major) {
        return new Member(name, studentNumber, grade, skills, role, major);
    }

    // 비즈니스 로직
    public void updateProfile(String name, String profileImageUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.name = NameVo.of(name);
        this.profileImage = profileImageUrl != null ? new ProfileImageVo(profileImageUrl) : null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateSkills(java.util.List<String> skills) {
        this.skills = new SkillsVo(skills);
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateRole(MemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        this.role = role;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateGrade(MemberGrade grade) {
        this.grade = grade;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateMajor(String major) {
        this.major = MajorVo.of(major);
        this.updatedAt = OffsetDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean hasSkill(String skill) {
        return skills.values().contains(skill);
    }

    // Getters - Domain VOs 반환
    public Long getId() { return id; }
    public NameVo getName() { return name; }
    public StudentNumberVo getStudentNumber() { return studentNumber; }
    public ProfileImageVo getProfileImage() { return profileImage; }
    public MemberGrade getGrade() { return grade; }
    public MemberRole getRole() { return role; }
    public SkillsVo getSkills() { return skills; }
    public MajorVo getMajor() { return major; }
    public String getDescription() { return description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // Primitive 값 반환 메서드 (편의용)
    public String getNameValue() { return name != null ? name.value() : null; }
    public MemberRole getRoleValue() { return role != null ? role : null; }
    public MemberGrade getGradeValue() { return grade != null ? grade : null; }
    public String getMajorValue() { return major != null ? major.value() : null; }
    public String getProfileImageValue() { return profileImage != null ? profileImage.value() : null;}
    public String getStudentNumberValue() { return studentNumber != null ? studentNumber.value() : null;}
}