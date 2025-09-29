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

    @Builder
    private MemberPenaltyEntity(Long memberId, Integer penaltyPoint, OffsetDateTime penaltiedAt, OffsetDateTime updatedAt) {
        this.memberId = memberId;
        this.penaltyPoint = penaltyPoint;
        this.penaltiedAt = penaltiedAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 패널티 점수 갱신 (초기화 방식)
     */
    public void updatePenaltyPoints(Integer points) {
        if (points < 0) {
            throw new IllegalArgumentException("패널티 점수는 0 이상이어야 합니다");
        }
        this.penaltyPoint = points; // 덧셈에서 초기화 방식으로 변경
        this.penaltiedAt = OffsetDateTime.now();
    }
}