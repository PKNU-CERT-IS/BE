package org.certis.studyplatform.member.domain;

import org.certis.studyplatform.member.domain.vo.*;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Deprecated(forRemoval = true)
@Builder
@Getter
public class Member {
    private final Long id;
    private NameVo name;
    private final StudentNumberVo studentNumber;
    private ProfileImageVo profileImage;
    private GradeVo grade;
    private RoleVo role;
    private SkillsVo skills;
    private MajorVo major;
    private String description;
    private final ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // 새 회원 생성
    public Member(String name, String studentNumber, String grade,
                  java.util.List<String> skills, String role, String major) {
        this.id = null;
        this.name = NameVo.of(name);
        this.studentNumber = new StudentNumberVo(studentNumber);
        this.profileImage = null;
        this.grade = GradeVo.of(grade);
        this.role = RoleVo.of(role);
        this.skills = new SkillsVo(skills);
        this.major = MajorVo.of(major);
        this.description = null;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // 기존 회원 복원 (도메인 VO 버전)
    public Member(Long id, NameVo name, StudentNumberVo studentNumber, 
                  ProfileImageVo profileImage, GradeVo grade, RoleVo role, 
                  SkillsVo skills, MajorVo major, String description,
                  ZonedDateTime createdAt, ZonedDateTime updatedAt) {
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

    // Factory method for domain creation
    @Builder
    public static Member create(String name, String studentNumber, String grade,
                              java.util.List<String> skills, String role, String major) {
        return new Member(name, studentNumber, grade, skills, role, major);
    }

    // 비즈니스 로직
    public void updateProfile(String name, String profileImageUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.name = NameVo.of(name);
        this.profileImage = profileImageUrl != null ? new ProfileImageVo(profileImageUrl) : null;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateSkills(java.util.List<String> skills) {
        this.skills = new SkillsVo(skills);
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        this.role = RoleVo.of(role);
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateGrade(String grade) {
        this.grade = GradeVo.of(grade);
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateMajor(String major) {
        this.major = MajorVo.of(major);
        this.updatedAt = ZonedDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean hasSkill(String skill) {
        return skills.values().contains(skill);
    }

    // Primitive 값 반환 메서드 (편의용)
    public String getNameValue() { return name != null ? name.value() : null; }
    public String getRoleValue() { return role != null ? role.value() : null; }
    public String getGradeValue() { return grade != null ? grade.value() : null; }
    public String getMajorValue() { return major != null ? major.value() : null; }
    public String getProfileImageValue() { return profileImage != null ? profileImage.value() : null;}
    public String getStudentNumberValue() { return studentNumber != null ? studentNumber.value() : null;}
} 