package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Member Contact Entity
 *
 * 회원 연락처 정보를 저장하는 JPA Entity
 * Member와 1:1 관계
 */
@Entity
@Table(name = "member_contact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberContactEntity {

    @Id
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(toBuilder = true)
    private MemberContactEntity(Long memberId, String email,
                               String githubUrl, String linkedinUrl, String phoneNumber, OffsetDateTime updatedAt) {
        this.memberId = memberId;
        this.email = email;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.phoneNumber = phoneNumber;
        this.updatedAt = updatedAt;
    }
}