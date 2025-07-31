package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.certis.studyplatform.member.domain.model.Member;

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

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "text[]")
    private String[] skills;

    private String major;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Builder(toBuilder = true)
    private MemberEntity(Long id, String name, String studentNumber, String profileImage,
                         String grade, String role, String[] skills, String major,
                         ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
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
        this.deletedAt = deletedAt;
    }

    // Domain → Entity 변환
    public static MemberEntity fromDomain(Member member) {
        return MemberEntity.builder()
                .id(member.getId() != null ? member.getId().value() : null)
                .name(member.getName())
                .studentNumber(member.getStudentNumber().value())
                .profileImage(member.getProfileImage() != null ? member.getProfileImage().value() : null)
                .grade(member.getGrade())
                .role(member.getRole())
                .skills(member.getSkills().toArray())
                .major(member.getMajor())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    // Entity → Domain 변환
    public Member toDomain() {
        return new Member(
                this.id != null ? new MemberId(this.id) : null,
                this.name,
                new StudentNumber(this.studentNumber),
                this.profileImage != null ? new ProfileImage(this.profileImage) : null,
                this.grade,
                this.role,
                this.skills != null ? Skills.of(Arrays.asList(this.skills)) : Skills.empty(),
                this.major,
                this.createdAt,
                this.updatedAt
        );
    }
}