package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 회원 요약 정보 Value Object
 * 
 * 회원 목록 조회 시 사용되는 요약 정보를 담는 불변 객체
 * 목록 표시에 필요한 핵심 정보만 포함하여 성능 최적화
 */
public record MemberSummaryVo(
        MemberIdVo id,                    // 회원 ID
        NameVo name,                      // 이름
        StudentNumberVo studentNumber,    // 학번
        GradeVo grade,                    // 학년
        RoleVo role,                      // 역할
        MajorVo major,                    // 전공
        String description,               // 설명 (nullable)
        SkillsVo skills,                  // 기술 스택
        ProfileImageVo profileImage,      // 프로필 이미지 (nullable)
        ZonedDateTime createdAt           // 생성일시
) {
    
    public MemberSummaryVo {
        if (id == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
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
        if (skills == null) {
            throw new IllegalArgumentException("기술 스택은 필수입니다");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("생성일시는 필수입니다");
        }
    }
    
    /**
     * 정적 팩토리 메서드 (모든 필드 포함)
     */
    public static MemberSummaryVo of(MemberIdVo id, NameVo name, StudentNumberVo studentNumber,
                                     GradeVo grade, RoleVo role, MajorVo major,
                                     String description, SkillsVo skills,
                                     ProfileImageVo profileImage, ZonedDateTime createdAt) {
        return new MemberSummaryVo(id, name, studentNumber, grade, role, major,
                description, skills, profileImage, createdAt);
    }
    
    /**
     * 정적 팩토리 메서드 (필수 필드만)
     */
    public static MemberSummaryVo of(MemberIdVo id, NameVo name, StudentNumberVo studentNumber,
                                     GradeVo grade, RoleVo role, MajorVo major,
                                     SkillsVo skills, ZonedDateTime createdAt) {
        return new MemberSummaryVo(id, name, studentNumber, grade, role, major,
                null, skills, null, createdAt);
    }
    
    /**
     * 정적 팩토리 메서드 (primitive 값들로)
     */
    public static MemberSummaryVo of(Long id, String name, String studentNumber,
                                     String grade, String role, String major,
                                     String description, List<String> skills,
                                     String profileImage, ZonedDateTime createdAt) {
        return new MemberSummaryVo(
            MemberIdVo.of(id),
            NameVo.of(name),
            new StudentNumberVo(studentNumber),
            GradeVo.of(grade),
            RoleVo.of(role),
            MajorVo.of(major),
            description,
            new SkillsVo(skills),
            profileImage != null ? ProfileImageVo.of(profileImage) : null,
            createdAt
        );
    }
    
    // =================================================================
    // Getter 메서드들
    // =================================================================
    
    /**
     * 회원 ID VO 반환
     */
    public MemberIdVo getId() {
        return id;
    }
    
    /**
     * 이름 VO 반환
     */
    public NameVo getName() {
        return name;
    }
    
    /**
     * 학번 VO 반환
     */
    public StudentNumberVo getStudentNumber() {
        return studentNumber;
    }
    
    /**
     * 학년 VO 반환
     */
    public GradeVo getGrade() {
        return grade;
    }
    
    /**
     * 역할 VO 반환
     */
    public RoleVo getRole() {
        return role;
    }
    
    /**
     * 전공 VO 반환
     */
    public MajorVo getMajor() {
        return major;
    }
    
    /**
     * 설명 반환
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 기술스택 VO 반환
     */
    public SkillsVo getSkills() {
        return skills;
    }
    
    /**
     * 프로필 이미지 VO 반환
     */
    public ProfileImageVo getProfileImage() {
        return profileImage;
    }
    
    /**
     * 생성일시 반환
     */
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 회원 ID 반환
     */
    public Long getMemberId() {
        return id.value();
    }
    
    /**
     * 이름 반환
     */
    public String getNameValue() {
        return name.value();
    }
    
    /**
     * 학번 반환
     */
    public String getStudentNumberValue() {
        return studentNumber.value();
    }
    
    /**
     * 학년 반환
     */
    public String getGradeValue() {
        return grade.value();
    }
    
    /**
     * 역할 반환
     */
    public String getRoleValue() {
        return role.value();
    }
    
    /**
     * 전공 반환
     */
    public String getMajorValue() {
        return major.value();
    }
    
    /**
     * 기술 스택 목록 반환
     */
    public java.util.List<String> getSkillsValues() {
        return skills.values();
    }
    
    /**
     * 프로필 이미지 URL 반환 (nullable)
     */
    public String getProfileImageValue() {
        return profileImage != null ? profileImage.value() : null;
    }
    
    /**
     * 설명이 있는지 확인
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    /**
     * 프로필 이미지가 있는지 확인
     */
    public boolean hasProfileImage() {
        return profileImage != null;
    }
    
    /**
     * 기술 스택 개수 반환
     */
    public int getSkillsCount() {
        return skills.values().size();
    }
}
