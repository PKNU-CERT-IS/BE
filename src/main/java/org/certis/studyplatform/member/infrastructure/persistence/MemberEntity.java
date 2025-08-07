package org.certis.studyplatform.member.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.certis.studyplatform.member.domain.Member;

import java.time.ZonedDateTime;
import java.util.Arrays;

@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE member SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(nullable = false)
    private String grade;

    @Enumerated(EnumType.STRING) // 중요!!
    @Column(nullable = false)
    private MemberRole role;

    //TODO: String[]
    @Column(columnDefinition = "text[]")
    private Object skills;

    @Column(nullable = false)
    private String major;

    @Column(name = "birthday", nullable = false)
    private ZonedDateTime birthday;

    @Column(name = "gender", nullable = false)
    private String gender;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Builder(toBuilder = true)
    private MemberEntity(Long id, String name, String description, String studentNumber, String profileImage, 
                        String grade, MemberRole role, Object skills, String major,
                        ZonedDateTime birthday, String gender,
                        ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.studentNumber = studentNumber;
        this.profileImage = profileImage;
        this.grade = grade;
        this.role = role;
        this.skills = skills;
        this.major = major;
        this.birthday = birthday;
        this.gender = gender;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    // Domain → Entity 변환
    public static MemberEntity fromDomain(Member member) {
        return MemberEntity.builder()
                .id(member.getId() != null ? member.getId().value() : null)
                .name(member.getName().value())
                .description(member.getDescription())
                .studentNumber(member.getStudentNumber().value())
                .profileImage(member.getProfileImage() != null ? member.getProfileImage().value() : null)
                .grade(member.getGrade().value())
                .role(member.getRole())
                .skills(member.getSkills().toArray())
                .major(member.getMajor().value())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    // Entity → Domain 변환
    public Member toDomain() {
        return new Member(
                this.id != null ? new org.certis.studyplatform.member.domain.vo.MemberIdVo(this.id) : null,
                this.name,
                new org.certis.studyplatform.member.domain.vo.StudentNumberVo(this.studentNumber),
                this.profileImage != null ? new org.certis.studyplatform.member.domain.vo.ProfileImageVo(this.profileImage) : null,
                this.grade,
                this.role,
                new org.certis.studyplatform.member.domain.vo.SkillsVo(parseSkills(this.skills)),
                this.major,
                this.createdAt,
                this.updatedAt
        );
    }

    private java.util.List<String> parseSkills(Object skills) {
        if (skills == null) {
            return java.util.List.of();
        }
        
        // PostgreSQL array 처리
        String skillsString = skills.toString();
        if (skillsString.startsWith("{") && skillsString.endsWith("}")) {
            skillsString = skillsString.substring(1, skillsString.length() - 1);
            return Arrays.asList(skillsString.split(","));
        }
        
        return java.util.List.of(skillsString);
    }
} 