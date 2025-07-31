package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import org.certis.studyplatform.member.domain.model.vo.*;
import org.certis.studyplatform.member.domain.model.MemberContact;

import java.time.ZonedDateTime;

@Entity
@Table(name = "member_contact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberContactEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    private String description;

    private String email;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Builder
    private MemberContactEntity(Long memberId, String description, String email,
                                String githubUrl, String linkedinUrl, ZonedDateTime updatedAt) {
        this.memberId = memberId;
        this.description = description;
        this.email = email;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.updatedAt = updatedAt;
    }

    public static MemberContactEntity fromDomain(MemberContact contact, Long memberId) {
        return MemberContactEntity.builder()
                .memberId(memberId)
                .description(contact.getDescription())
                .email(contact.getEmail() != null ? contact.getEmail().value() : null)
                .githubUrl(contact.getGithubUrl())
                .linkedinUrl(contact.getLinkedinUrl())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    public MemberContact toDomain() {
        return new MemberContact(
                this.description,
                this.email != null ? new EmailVo(this.email) : null,
                this.githubUrl,
                this.linkedinUrl,
                this.updatedAt
        );
    }
}