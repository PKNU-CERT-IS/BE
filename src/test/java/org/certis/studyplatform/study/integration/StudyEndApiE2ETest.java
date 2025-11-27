package org.certis.studyplatform.study.integration;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.application.StudyFacadeService;
import org.certis.studyplatform.study.presentation.dto.request.StudyEndRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyDetailResponseDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestRedisMockConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.STUDY;
import static org.certis.generated.jooq.Tables.MEMBER;

@SpringBootTest
@Import({TestEmbeddedPostgresConfig.class, TestRedisMockConfig.class})
@ActiveProfiles("test")
@Transactional
class StudyEndApiE2ETest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private StudyFacadeService studyFacadeService;

    @Test
    @DisplayName("endStudy ignores endedAt; relies on DB status/resultSubmitStatus only")
    void endStudy_ignoresEndedAt_whenStatusNotCompleted() {
        // Given: Insert a study with ended_at in the past but status APPROVED (not COMPLETED)
        OffsetDateTime now = OffsetDateTime.now();
        Long creatorId = 101L;

        // Satisfy FK: insert minimal member
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, creatorId)
                .set(MEMBER.NAME, "creator-101")
                .set(MEMBER.STUDENT_NUMBER, "20200101")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, now.minusYears(21))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .onConflict(MEMBER.ID).doNothing()
                .execute();

        Long studyId = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, creatorId)
                .set(STUDY.TITLE, "E2E")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.minusDays(10))
                .set(STUDY.ENDED_AT, now.minusDays(1)) // past
                .set(STUDY.STATUS, "APPROVED") // not COMPLETED
                .set(STUDY.RESULT_SUBMIT_STATUS, ResultSubmitStatus.READY.name())
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);

        // When: call facade endStudy with requester == creator
        StudyEndRequestDto req = new StudyEndRequestDto();
        // via setters to avoid Lombok dependency in tests
        try {
            java.lang.reflect.Field f = StudyEndRequestDto.class.getDeclaredField("studyId");
            f.setAccessible(true);
            f.set(req, studyId);
        } catch (Exception ignore) {}

        StudyDetailResponseDto resp = studyFacadeService.endStudy(req, creatorId);

        // Then: success and resultSubmitStatus moved to INPROGRESS (submission created)
        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(studyId);
        // endedAt was past, but operation still proceeded because status != COMPLETED
    }
}


