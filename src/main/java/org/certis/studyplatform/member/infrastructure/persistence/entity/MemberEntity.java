package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZonedDateTime;
import java.util.Arrays;

@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE member SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @Column(columnDefinition = "text[]")
    private String[] skills;

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
                         String grade, MemberRole role, String[] skills, String major,
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

    // =================================================================
    // equals, hashCode, toString
    // =================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MemberEntity that = (MemberEntity) obj;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MemberEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", studentNumber='" + studentNumber + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
} 