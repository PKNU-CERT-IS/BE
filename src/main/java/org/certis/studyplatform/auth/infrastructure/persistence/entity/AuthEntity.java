package org.certis.studyplatform.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

//import org.certis.studyplatform.auth.domain.model.Auth;
//import org.certis.studyplatform.auth.domain.vo.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "auth")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SQLDelete(sql = "UPDATE auth SET deleted_at = NOW() WHERE member_id = ?")
@Where(clause = "deleted_at IS NULL")
public class AuthEntity {

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Id
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Builder
    private AuthEntity(String accountNumber, String password, ZonedDateTime createdAt,
                       ZonedDateTime updatedAt, ZonedDateTime deletedAt, Long memberId) {
        this.accountNumber = accountNumber;
        this.password = password;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.memberId = memberId;
    }

//    public static AuthEntity fromDomain(Auth auth) {
//        return AuthEntity.builder()
//                .accountNumber(auth.getAccountNumber().value())
//                .password(auth.getPassword())
//                .createdAt(auth.getCreatedAt())
//                .updatedAt(auth.getUpdatedAt())
//                .memberId(auth.getMemberId().value())
//                .build();
//    }
//
//    public Auth toDomain() {
//        return new Auth(
//                new AccountNumber(this.accountNumber),
//                this.password,
//                this.createdAt,
//                this.updatedAt,
//                new MemberId(this.memberId)
//        );
//    }
}
