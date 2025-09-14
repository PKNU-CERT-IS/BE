package org.certis.studyplatform.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

/**
 * Auth Entity
 *
 * 회원 인증 정보를 저장하는 JPA Entity
 * Member와 1:1 관계를 가짐
 */
@Entity
@Table(name = "auth")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE auth SET deleted_at = NOW() WHERE member_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class AuthEntity {

    @Id
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;  // member_id를 PK로 사용

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    // Member와의 관계 매핑 추가
    @OneToOne
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private MemberEntity member;

    @Column(name = "password", nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Builder(toBuilder = true)
    private AuthEntity(String accountNumber, String password, OffsetDateTime createdAt,
                      OffsetDateTime updatedAt, OffsetDateTime deletedAt, Long memberId) {
        this.accountNumber = accountNumber;
        this.password = password;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.memberId = memberId;
    }

    public void setPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
        // 예외처리
        }
        this.password = newPassword;
    }
}