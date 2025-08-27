package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Member Penalty Entity
 *
 * 회원 패널티 정보를 저장하는 JPA Entity
 * Member와 1:1 관계
 */
@Entity
@Table(name = "member_penalty")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberPenaltyEntity {

    @Id
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "penalty_point", nullable = false)
    private Integer penaltyPoint;

    @Column(name = "penaltied_at", nullable = false)
    private OffsetDateTime penaltiedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Builder(toBuilder = true)
    private MemberPenaltyEntity(Long memberId, Integer penaltyPoint, OffsetDateTime penaltiedAt,
                               OffsetDateTime updatedAt, String reason) {
        this.memberId = memberId;
        this.penaltyPoint = penaltyPoint;
        this.penaltiedAt = penaltiedAt;
        this.updatedAt = updatedAt;
        this.reason = reason;
    }
}