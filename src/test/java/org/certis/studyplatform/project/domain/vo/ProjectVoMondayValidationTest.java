package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProjectVo 월요일/일요일 검증 테스트")
class ProjectVoMondayValidationTest {

    @Nested
    @DisplayName("시작일 요일 검증")
    class StartDayValidation {
        @Test
        @DisplayName("월요일 시작일은 허용")
        void mondayStart_shouldPass() {
            OffsetDateTime monday = next(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime sunday = monday.plusWeeks(4).with(DayOfWeek.SUNDAY);

            assertThatCode(() -> new ProjectVo(
                    null, "title", "desc", "content", "SECURITY", "WEB",
                    monday, sunday,
                    1L, "creator", "FRESHMAN", "2024-1", "READY",
                    ResultSubmitStatus.READY,
                    null, null, null, null,
                    5, 0, true,
                    Collections.emptyList(), Collections.emptyList()
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("월요일이 아닌 시작일은 예외")
        void nonMondayStart_shouldThrow() {
            OffsetDateTime tuesday = next(DayOfWeek.TUESDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime sunday = tuesday.plusWeeks(4).with(DayOfWeek.SUNDAY);

            assertThatThrownBy(() -> new ProjectVo(
                    null, "title", "desc", "content", "SECURITY", "WEB",
                    tuesday, sunday,
                    1L, "creator", "FRESHMAN", "2024-1", "READY",
                    ResultSubmitStatus.READY,
                    null, null, null, null,
                    5, 0, true,
                    Collections.emptyList(), Collections.emptyList()
            )).isInstanceOf(DomainException.class)
             .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_INVALID_START_DAY);
        }
    }

    @Nested
    @DisplayName("종료일 요일 검증")
    class EndDayValidation {
        @Test
        @DisplayName("일요일 종료일은 허용")
        void sundayEnd_shouldPass() {
            OffsetDateTime monday = next(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime sunday = monday.plusWeeks(4).with(DayOfWeek.SUNDAY);

            assertThatCode(() -> new ProjectVo(
                    null, "title", "desc", "content", "SECURITY", "WEB",
                    monday, sunday,
                    1L, "creator", "FRESHMAN", "2024-1", "READY",
                    ResultSubmitStatus.READY,
                    null, null, null, null,
                    5, 0, true,
                    Collections.emptyList(), Collections.emptyList()
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일요일이 아닌 종료일은 예외")
        void nonSundayEnd_shouldThrow() {
            OffsetDateTime monday = next(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime friday = monday.plusWeeks(4).with(DayOfWeek.FRIDAY);

            assertThatThrownBy(() -> new ProjectVo(
                    null, "title", "desc", "content", "SECURITY", "WEB",
                    monday, friday,
                    1L, "creator", "FRESHMAN", "2024-1", "READY",
                    ResultSubmitStatus.READY,
                    null, null, null, null,
                    5, 0, true,
                    Collections.emptyList(), Collections.emptyList()
            )).isInstanceOf(DomainException.class)
             .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_INVALID_END_DAY);
        }
    }

    private OffsetDateTime next(DayOfWeek dayOfWeek) {
        OffsetDateTime now = OffsetDateTime.now();
        int shift = dayOfWeek.getValue() - now.getDayOfWeek().getValue();
        if (shift <= 0) shift += 7;
        return now.plusDays(shift);
    }
}


