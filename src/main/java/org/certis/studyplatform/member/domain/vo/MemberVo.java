package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Member Response VO Record
 * 불변성이 보장된 읽기 전용 값 객체
 */
public record MemberVo(
        Long id,
        String name,
        String studentNumber,
        String profileImage,
        MemberGrade grade,
        MemberRole role,
        List<String> skills,
        String major,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    // 방어적 복사를 위한 생성자
    public MemberVo {
        skills = skills != null ? List.copyOf(skills) : List.of();
    }

    /**
     * 정적 팩토리 메서드
     */
    public static MemberVo of(Long id, String name, String studentNumber, String profileImage,
                              MemberGrade grade, MemberRole role, List<String> skills, String major,
                              String description, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new MemberVo(id, name, studentNumber, profileImage, grade, role, skills, major, description, createdAt, updatedAt);
    }

    // =================================================================
    // Getter 메서드들
    // =================================================================

    /**
     * ID 반환
     */
    public Long getId() {
        return id;
    }

    /**
     * 이름 반환
     */
    public String getName() {
        return name;
    }

    /**
     * 학번 반환
     */
    public String getStudentNumber() {
        return studentNumber;
    }

    /**
     * 프로필 이미지 반환
     */
    public String getProfileImage() {
        return profileImage;
    }

    /**
     * 학년 반환
     */
    public MemberGrade getGrade() {
        return grade;
    }

    /**
     * 역할 반환
     */
    public MemberRole getRole() {
        return role;
    }

    /**
     * 기술스택 반환
     */
    public List<String> getSkills() {
        return skills;
    }

    /**
     * 전공 반환
     */
    public String getMajor() {
        return major;
    }

    /**
     * 설명 반환
     */
    public String getDescription() {
        return description;
    }

    /**
     * 생성일시 반환
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 수정일시 반환
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 편의 메서드
    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }
}