package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
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
        String grade,
        String role,
        List<String> skills,
        String major,
        String description,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    // 방어적 복사를 위한 생성자
    public MemberVo {
        skills = skills != null ? List.copyOf(skills) : List.of();
    }

    /**
     * 정적 팩토리 메서드
     */
    public static MemberVo of(Long id, String name, String studentNumber, String profileImage,
                              String grade, String role, List<String> skills, String major,
                              String description, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
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
    public String getGrade() {
        return grade;
    }
    
    /**
     * 역할 반환
     */
    public String getRole() {
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
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 수정일시 반환
     */
    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 편의 메서드
    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }
}