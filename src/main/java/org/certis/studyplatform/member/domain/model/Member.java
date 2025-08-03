package org.certis.studyplatform.member.domain.model;

import org.certis.studyplatform.member.domain.model.vo.*;
import java.time.ZonedDateTime;

public class Member {
    private final MemberIdVo id;
    private String name;
    private final StudentNumberVo studentNumber;
    private ProfileImageVo profileImage;
    private String grade;
    private String role;
    private SkillsVo skills;
    private String major;
    private final ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // 새 회원 생성
    public Member(String name, StudentNumberVo studentNumber, String grade,
                  SkillsVo skills, String role, String major) {
        this.id = null;
        this.name = name;
        this.studentNumber = studentNumber;
        this.profileImage = null;
        this.grade = grade;
        this.role = role;
        this.skills = skills.empty();
        this.major = major;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // 기존 회원 복원
    public Member(MemberIdVo id, String name, StudentNumberVo studentNumber, ProfileImageVo profileImage,
                  String grade, String role, SkillsVo skills, String major,
                  ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.studentNumber = studentNumber;
        this.profileImage = profileImage;
        this.grade = grade;
        this.role = role;
        this.skills = skills;
        this.major = major;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 비즈니스 로직
    public void updateProfile(String name, ProfileImageVo profileImage) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.name = name;
        this.profileImage = profileImage;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateSkills(SkillsVo skills) {
        this.skills = skills;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        this.role = role;
        this.updatedAt = ZonedDateTime.now();
    }

    public boolean hasSkill(String skill) {
        return skills.values().contains(skill);
    }

    // Getters
    public MemberIdVo getId() { return id; }
    public String getName() { return name; }
    public StudentNumberVo getStudentNumber() { return studentNumber; }
    public ProfileImageVo getProfileImage() { return profileImage; }
    public String getGrade() { return grade; }
    public String getRole() { return role; }
    public SkillsVo getSkills() { return skills; }
    public String getMajor() { return major; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
}