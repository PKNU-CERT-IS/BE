package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StudyVo 종료일 일요일 검증 테스트")
class StudyVoEndDateSundayValidationTest {

    @Test
    @DisplayName("종료일이 일요일이면 통과")
    void sundayEnd_shouldPass() {
        OffsetDateTime monday = next(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime sunday = monday.plusWeeks(4).with(DayOfWeek.SUNDAY);

        assertThatCode(() -> StudyVo.of(
                null, "title", "desc", "content", "SECURITY", "WEB",
                monday, sunday, OffsetDateTime.now(), OffsetDateTime.now(),
                1L, "creator", MemberGrade.FRESHMAN, null, "2024-1", "READY",
                ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료일이 일요일이 아니면 예외")
    void nonSundayEnd_shouldThrow() {
        OffsetDateTime monday = next(DayOfWeek.MONDAY).withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime saturday = monday.plusWeeks(4).with(DayOfWeek.SATURDAY);

        assertThatThrownBy(() -> StudyVo.of(
                null, "title", "desc", "content", "SECURITY", "WEB",
                monday, saturday, OffsetDateTime.now(), OffsetDateTime.now(),
                1L, "creator", MemberGrade.FRESHMAN, null, "2024-1", "READY",
                ResultSubmitStatus.READY, 5, 0, true, Collections.emptyList()
        )).isInstanceOf(DomainException.class)
         .hasFieldOrPropertyWithValue("status", ExceptionStatus.STUDY_DOMAIN_INVALID_END_DAY);
    }

    private OffsetDateTime next(DayOfWeek dayOfWeek) {
        OffsetDateTime now = OffsetDateTime.now();
        int shift = dayOfWeek.getValue() - now.getDayOfWeek().getValue();
        if (shift <= 0) shift += 7;
        return now.plusDays(shift);
    }
}


