package org.certis.studyplatform.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DateTimeUtils 테스트")
class DateTimeUtilsTest {

    @Nested
    @DisplayName("월요일 검증 테스트")
    class MondayValidationTest {

        @Test
        @DisplayName("월요일인 경우 true 반환")
        void isMonday_WhenMonday_ReturnsTrue() {
            // Given: 월요일 날짜 생성
            OffsetDateTime monday = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC); // 2024년 1월 1일은 월요일

            // When & Then
            assertThat(DateTimeUtils.isMonday(monday)).isTrue();
        }

        @Test
        @DisplayName("월요일이 아닌 경우 false 반환")
        void isMonday_WhenNotMonday_ReturnsFalse() {
            // Given: 화요일 날짜 생성
            OffsetDateTime tuesday = OffsetDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC); // 2024년 1월 2일은 화요일

            // When & Then
            assertThat(DateTimeUtils.isMonday(tuesday)).isFalse();
        }

        @Test
        @DisplayName("월요일인 경우 예외 발생하지 않음")
        void validateIsMonday_WhenMonday_DoesNotThrowException() {
            // Given: 월요일 날짜 생성
            OffsetDateTime monday = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

            // When & Then
            assertThatCode(() -> DateTimeUtils.validateIsMonday(monday))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("월요일이 아닌 경우 IllegalArgumentException 발생")
        void validateIsMonday_WhenNotMonday_ThrowsException() {
            // Given: 화요일 날짜 생성
            OffsetDateTime tuesday = OffsetDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC);

            // When & Then
            assertThatThrownBy(() -> DateTimeUtils.validateIsMonday(tuesday))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작일은 월요일이어야 합니다")
                    .hasMessageContaining("TUESDAY");
        }

        @Test
        @DisplayName("다양한 요일에 대한 검증")
        void validateIsMonday_VariousDaysOfWeek() {
            // Given: 각 요일별 날짜 생성
            OffsetDateTime monday = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime tuesday = OffsetDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime wednesday = OffsetDateTime.of(2024, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime thursday = OffsetDateTime.of(2024, 1, 4, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime friday = OffsetDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime saturday = OffsetDateTime.of(2024, 1, 6, 10, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime sunday = OffsetDateTime.of(2024, 1, 7, 10, 0, 0, 0, ZoneOffset.UTC);

            // When & Then
            assertThat(DateTimeUtils.isMonday(monday)).isTrue();
            assertThat(DateTimeUtils.isMonday(tuesday)).isFalse();
            assertThat(DateTimeUtils.isMonday(wednesday)).isFalse();
            assertThat(DateTimeUtils.isMonday(thursday)).isFalse();
            assertThat(DateTimeUtils.isMonday(friday)).isFalse();
            assertThat(DateTimeUtils.isMonday(saturday)).isFalse();
            assertThat(DateTimeUtils.isMonday(sunday)).isFalse();
        }
    }
}
