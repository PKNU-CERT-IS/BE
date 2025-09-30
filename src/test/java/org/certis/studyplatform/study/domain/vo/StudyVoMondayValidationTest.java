package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StudyVo 월요일 검증 테스트")
class StudyVoMondayValidationTest {

    @Nested
    @DisplayName("월요일 검증 테스트")
    class MondayValidationTest {

        @Test
        @DisplayName("월요일 시작일로 스터디 생성 시 정상 처리")
        void createStudyWithMondayStartDate_ShouldPass() {
            // Given: 미래 월요일 시작일
            OffsetDateTime mondayStart = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime endDate = mondayStart.plusWeeks(4);

            // When & Then
            assertThatCode(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    mondayStart, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("월요일이 아닌 시작일로 스터디 생성 시 예외 발생")
        void createStudyWithNonMondayStartDate_ShouldThrowException() {
            // Given: 미래 화요일 시작일
            OffsetDateTime tuesdayStart = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.TUESDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime endDate = tuesdayStart.plusWeeks(4);

            // When & Then
            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    tuesdayStart, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작일은 월요일이어야 합니다");
        }

        @Test
        @DisplayName("다양한 요일에 대한 검증")
        void validateVariousDaysOfWeek() {
            // Given: 각 요일별 미래 시작일
            OffsetDateTime monday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime tuesday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.TUESDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime wednesday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.WEDNESDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime thursday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.THURSDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime friday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.FRIDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime saturday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.SATURDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime sunday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.SUNDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);

            OffsetDateTime endDate = monday.plusWeeks(4);

            // When & Then
            assertThatCode(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    monday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).doesNotThrowAnyException();

            // 월요일이 아닌 모든 요일은 예외 발생
            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    tuesday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    wednesday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    thursday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    friday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    saturday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> StudyVo.of(
                    null, "테스트 스터디", "설명", "내용", "카테고리", "하위카테고리",
                    sunday, endDate, OffsetDateTime.now(), OffsetDateTime.now(),
                    1L, "생성자", MemberGrade.SENIOR, "2024-01", "READY",
                    ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
