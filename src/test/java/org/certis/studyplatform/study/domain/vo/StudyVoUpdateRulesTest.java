package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class StudyVoUpdateRulesTest {

    private StudyVo createBaseStudy(String status, OffsetDateTime start, OffsetDateTime end) {
        return StudyVo.createForTest(
                1L,
                "Title",
                "Description",
                "Content",
                "Category",
                "Sub",
                start,
                end,
                OffsetDateTime.now().minusDays(30),
                OffsetDateTime.now().minusDays(1),
                10L,
                "Creator",
                MemberGrade.NONE,
                null,
                "2025-2",
                status,
                10,
                0,
                true,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    void allowsChangingStartDateWhenApproved() {
        OffsetDateTime now = OffsetDateTime.now();
        StudyVo existing = createBaseStudy("APPROVED", now.plusDays(1), now.plusDays(30));

        OffsetDateTime newStart = now.plusDays(3);
        assertDoesNotThrow(() -> StudyVo.updateFrom(existing,
                null, null, null, null, null,
                newStart, existing.endDate(), null));
    }

    @Test
    void blocksChangingStartDateWhenRejected() {
        OffsetDateTime now = OffsetDateTime.now();
        StudyVo existing = createBaseStudy("REJECTED", now.plusDays(1), now.plusDays(30));

        OffsetDateTime newStart = now.plusDays(3);
        assertThrows(DomainException.class, () -> StudyVo.updateFrom(existing,
                null, null, null, null, null,
                newStart, existing.endDate(), null));
    }

    @Test
    void inProgressBecomesApprovedWhenStartMovedToFuture() {
        OffsetDateTime now = OffsetDateTime.now();
        // INPROGRESS: start in the past, end in the future relative to now
        StudyVo existing = createBaseStudy("INPROGRESS", now.minusDays(2), now.plusDays(20));

        OffsetDateTime futureStart = now.plusDays(7);
        StudyVo updated = StudyVo.updateFrom(existing,
                null, null, null, null, null,
                futureStart, existing.endDate(), null);

        assertEquals("APPROVED", updated.status());
        assertEquals(ResultSubmitStatus.READY, updated.resultSubmitStatus());
        assertEquals(futureStart, updated.startDate());
    }
}


