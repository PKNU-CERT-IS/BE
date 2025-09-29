package org.certis.studyplatform.member.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MemberPenaltyEntity 테스트")
class MemberPenaltyEntityTest {

    @Nested
    @DisplayName("벌점 초기화 방식 테스트")
    class PenaltyInitializationTest {

        @Test
        @DisplayName("벌점을 초기화 방식으로 업데이트")
        void updatePenaltyPoints_ShouldInitializeValue() {
            // Given: 초기 벌점이 3점인 엔티티
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(3)
                    .penaltiedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            // When: 5점으로 업데이트 (덧셈이 아닌 초기화)
            entity.updatePenaltyPoints(5);

            // Then: 벌점이 5점으로 초기화됨 (3 + 5 = 8이 아님)
            assertThat(entity.getPenaltyPoint()).isEqualTo(5);
        }

        @Test
        @DisplayName("벌점을 0점으로 초기화")
        void updatePenaltyPoints_WithZero_ShouldInitializeToZero() {
            // Given: 초기 벌점이 5점인 엔티티
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(5)
                    .penaltiedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            // When: 0점으로 업데이트
            entity.updatePenaltyPoints(0);

            // Then: 벌점이 0점으로 초기화됨
            assertThat(entity.getPenaltyPoint()).isEqualTo(0);
        }

        @Test
        @DisplayName("음수 벌점은 예외 발생")
        void updatePenaltyPoints_WithNegative_ShouldThrowException() {
            // Given: 초기 벌점이 3점인 엔티티
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(3)
                    .penaltiedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            // When & Then: 음수 벌점은 예외 발생
            assertThatThrownBy(() -> entity.updatePenaltyPoints(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("패널티 점수는 0 이상이어야 합니다");
        }

        @Test
        @DisplayName("벌점 업데이트 시 penaltiedAt이 현재 시간으로 설정됨")
        void updatePenaltyPoints_ShouldUpdatePenaltiedAt() {
            // Given: 초기 시간이 과거인 엔티티
            OffsetDateTime pastTime = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(3)
                    .penaltiedAt(pastTime)
                    .updatedAt(pastTime)
                    .build();

            // When: 벌점 업데이트
            entity.updatePenaltyPoints(5);

            // Then: penaltiedAt이 현재 시간으로 업데이트됨
            assertThat(entity.getPenaltiedAt()).isAfter(pastTime);
        }

        @Test
        @DisplayName("여러 번 업데이트해도 항상 초기화 방식으로 동작")
        void multipleUpdates_ShouldAlwaysInitialize() {
            // Given: 초기 벌점이 0점인 엔티티
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(0)
                    .penaltiedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            // When: 여러 번 업데이트
            entity.updatePenaltyPoints(3);
            entity.updatePenaltyPoints(7);
            entity.updatePenaltyPoints(2);

            // Then: 마지막 값인 2점으로 설정됨 (덧셈이 아님)
            assertThat(entity.getPenaltyPoint()).isEqualTo(2);
        }

        @Test
        @DisplayName("기존 덧셈 방식과의 차이점 확인")
        void comparisonWithOldAdditionMethod() {
            // Given: 초기 벌점이 2점인 엔티티
            MemberPenaltyEntity entity = MemberPenaltyEntity.builder()
                    .memberId(1L)
                    .penaltyPoint(2)
                    .penaltiedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            // When: 3점으로 업데이트
            entity.updatePenaltyPoints(3);

            // Then: 새로운 방식(초기화)에서는 3점, 기존 방식(덧셈)에서는 5점이 됨
            assertThat(entity.getPenaltyPoint()).isEqualTo(3); // 새로운 방식
            assertThat(entity.getPenaltyPoint()).isNotEqualTo(5); // 기존 방식과 다름
        }
    }
}
