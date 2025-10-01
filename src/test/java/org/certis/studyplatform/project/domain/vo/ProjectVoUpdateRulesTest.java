package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ProjectVoUpdateRulesTest {

    private ProjectVo createBaseProject(String status, OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime kstMondayStart = alignToKstMonday(start);
        OffsetDateTime kstSundayEnd = alignToKstSunday(end.isAfter(kstMondayStart) ? end : kstMondayStart.plusDays(6));
        return ProjectVo.of(
                1L,
                "Title",
                "Description",
                "Content",
                "Category",
                "Sub",
                kstMondayStart,
                kstSundayEnd,
                10L,
                "Creator",
                MemberGrade.NONE.getDescription(),
                "2025-2",
                status,
                null,
                null,
                null,
                null,
                10,
                0,
                true,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    void allowsChangingStartDateWhenApproved() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = alignToKstMonday(now.plusDays(7));
        OffsetDateTime end = alignToKstSunday(start.plusDays(27));
        ProjectVo existing = createBaseProject("APPROVED", start, end);

        OffsetDateTime newStart = alignToKstMonday(now.plusDays(14));
        assertDoesNotThrow(() -> ProjectVo.updateFrom(existing,
                null, null, null, null, null,
                newStart, existing.endDate(), null, null, null, null, null));
    }

    @Test
    void blocksChangingStartDateWhenRejected() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = alignToKstMonday(now.plusDays(7));
        OffsetDateTime end = alignToKstSunday(start.plusDays(27));
        ProjectVo existing = createBaseProject("REJECTED", start, end);

        OffsetDateTime newStart = alignToKstMonday(now.plusDays(14));
        assertThrows(DomainException.class, () -> ProjectVo.updateFrom(existing,
                null, null, null, null, null,
                newStart, existing.endDate(), null, null, null, null, null));
    }

    @Test
    void inProgressBecomesApprovedWhenStartMovedToFuture() {
        OffsetDateTime now = OffsetDateTime.now();
        // INPROGRESS: start in the past (KST Monday), end in the future (KST Sunday)
        OffsetDateTime pastStart = alignToKstMonday(now.minusDays(14));
        OffsetDateTime futureEnd = alignToKstSunday(now.plusDays(14));
        ProjectVo existing = createBaseProject("INPROGRESS", pastStart, futureEnd);

        OffsetDateTime futureStart = alignToKstMonday(now.plusDays(14));
        ProjectVo updated = ProjectVo.updateFrom(existing,
                null, null, null, null, null,
                futureStart, existing.endDate(), null, null, null, null, null);

        assertEquals("APPROVED", updated.status());
        assertEquals(ResultSubmitStatus.READY, updated.resultSubmitStatus());
        assertEquals(futureStart, updated.startDate());
    }

    private OffsetDateTime alignToKstMonday(OffsetDateTime source) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        var zdt = source.atZoneSameInstant(kst);
        int shift = java.time.DayOfWeek.MONDAY.getValue() - zdt.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        return zdt.plusDays(shift).toOffsetDateTime();
    }

    private OffsetDateTime alignToKstSunday(OffsetDateTime source) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        var zdt = source.atZoneSameInstant(kst);
        int shift = java.time.DayOfWeek.SUNDAY.getValue() - zdt.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        return zdt.plusDays(shift).withHour(23).withMinute(59).withSecond(59).withNano(0).toOffsetDateTime();
    }
}


