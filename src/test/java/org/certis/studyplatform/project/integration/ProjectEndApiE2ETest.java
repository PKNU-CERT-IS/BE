package org.certis.studyplatform.project.integration;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.project.application.ProjectFacadeService;
import org.certis.studyplatform.project.presentation.dto.request.ProjectEndRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectDetailResponseDto;
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
import static org.certis.generated.jooq.Tables.MEMBER;
import static org.certis.generated.jooq.Tables.PROJECT;

@SpringBootTest
@Import({TestEmbeddedPostgresConfig.class, TestRedisMockConfig.class})
@ActiveProfiles("test")
@Transactional
class ProjectEndApiE2ETest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ProjectFacadeService projectFacadeService;

    @Test
    @DisplayName("endProject ignores endedAt; relies on DB status/resultSubmitStatus only")
    void endProject_ignoresEndedAt_whenStatusNotCompleted() {
        // Given: Insert a project with ended_at in the past but status APPROVED (not COMPLETED)
        OffsetDateTime now = OffsetDateTime.now();
        Long creatorId = 303L;

        // Satisfy FK: insert minimal member
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, creatorId)
                .set(MEMBER.NAME, "creator-303")
                .set(MEMBER.STUDENT_NUMBER, "20200002")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, now.minusYears(21))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .onConflict(MEMBER.ID).doNothing()
                .execute();

        Long projectId = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, creatorId)
                .set(PROJECT.TITLE, "E2E Project")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.minusDays(10))
                .set(PROJECT.ENDED_AT, now.minusDays(1)) // past
                .set(PROJECT.STATUS, "APPROVED") // not COMPLETED
                .set(PROJECT.RESULT_SUBMIT_STATUS, ResultSubmitStatus.READY.name())
                .set(PROJECT.CREATED_AT, now.minusDays(11))
                .set(PROJECT.UPDATED_AT, now.minusDays(1))
                .returning(PROJECT.ID)
                .fetchOne()
                .get(PROJECT.ID);

        // When: call facade endProject with requester == creator
        ProjectEndRequestDto req = new ProjectEndRequestDto();
        try {
            java.lang.reflect.Field f = ProjectEndRequestDto.class.getDeclaredField("projectId");
            f.setAccessible(true);
            f.set(req, projectId);
        } catch (Exception ignore) {}

        ProjectDetailResponseDto resp = projectFacadeService.endProject(req, creatorId);

        // Then: success; endedAt past should not block since status != COMPLETED
        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(projectId);
    }
}



