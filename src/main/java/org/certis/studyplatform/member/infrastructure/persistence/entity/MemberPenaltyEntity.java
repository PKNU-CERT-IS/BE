package org.certis.studyplatform.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import org.certis.studyplatform.member.domain.model.MemberPenalty;

import java.time.ZonedDateTime;

@Entity
@Table(name = "member_penalty")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberPenaltyEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "penalty_point", nullable = false)
    private Integer penaltyPoint;

    @Column(name = "penaltied_at", nullable = false)
    private ZonedDateTime penaltiedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(nullable = false)
    private String reason;

    @Builder
    private MemberPenaltyEntity(Long memberId, Integer penaltyPoint, ZonedDateTime penaltiedAt,
                                ZonedDateTime updatedAt, String reason) {
        this.memberId = memberId;
        this.penaltyPoint = penaltyPoint;
        this.penaltiedAt = penaltiedAt;
        this.updatedAt = updatedAt;
        this.reason = reason;
    }

    public static MemberPenaltyEntity fromDomain(MemberPenalty penalty, Long memberId) {
        return MemberPenaltyEntity.builder()
                .memberId(memberId)
                .penaltyPoint(penalty.getPenaltyPoint())
                .penaltiedAt(penalty.getPenaltiedAt())
                .updatedAt(penalty.getUpdatedAt())
                .reason(penalty.getReason())
                .build();
    }

    public MemberPenalty toDomain() {
        return new MemberPenalty(
                this.penaltyPoint,
                this.penaltiedAt,
                this.updatedAt,
                this.reason
        );
    }
}