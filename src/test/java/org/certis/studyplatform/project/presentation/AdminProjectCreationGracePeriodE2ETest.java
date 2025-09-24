package org.certis.studyplatform.project.presentation;

import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.MEMBER;
import static org.certis.generated.jooq.Tables.PROJECT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("🧪 Project creation approve extends grace period for UPSOLVER and reject sets deleted_at")
class AdminProjectCreationGracePeriodE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long upsolverId;
    private Long staffId;
    private Long projectIdByUpsolver;
    private Long projectIdToReject;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        OffsetDateTime now = OffsetDateTime.now();

        // UPSOLVER member (eligible for grace period updates)
        upsolverId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "ups")
                .set(MEMBER.ROLE, "UPSOLVER")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240011")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.BIRTHDAY, now.minusYears(20))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);

        // STAFF admin
        staffId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "admin")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240012")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.BIRTHDAY, now.minusYears(21))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);

        // Project created by UPSOLVER (future dates → READY)
        projectIdByUpsolver = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, upsolverId)
                .set(PROJECT.TITLE, "P-UP")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, now.plusDays(2))
                .set(PROJECT.ENDED_AT, now.plusDays(30))
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .returning(PROJECT.ID)
                .fetchOne()
                .get(PROJECT.ID);

        // Project to be rejected
        projectIdToReject = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, upsolverId)
                .set(PROJECT.TITLE, "P-REJ")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, now.plusDays(3))
                .set(PROJECT.ENDED_AT, now.plusDays(31))
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .returning(PROJECT.ID)
                .fetchOne()
                .get(PROJECT.ID);
    }

    @Test
    @DisplayName("Creation approve extends member.grace_period for UPSOLVER")
    void approve_creation_extends_grace_period() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        // Before: no grace period
        var before = dsl.select(MEMBER.GRACE_PERIOD).from(MEMBER).where(MEMBER.ID.eq(upsolverId)).fetchOne(MEMBER.GRACE_PERIOD);
        assertThat(before).isNull();

        mockMvc.perform(post("/api/v1/admin/project/create/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("projectId", String.valueOf(projectIdByUpsolver)))
                .andDo(print())
                .andExpect(status().isOk());

        var after = dsl.select(MEMBER.GRACE_PERIOD).from(MEMBER).where(MEMBER.ID.eq(upsolverId)).fetchOne(MEMBER.GRACE_PERIOD);
        assertThat(after).isNotNull();
        assertThat(after).isAfter(OffsetDateTime.now().minusMinutes(1));
    }

    @Test
    @DisplayName("Creation reject sets deleted_at on project")
    void reject_creation_sets_deleted_at() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        mockMvc.perform(post("/api/v1/admin/project/create/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("projectId", String.valueOf(projectIdToReject)))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.selectFrom(PROJECT).where(PROJECT.ID.eq(projectIdToReject)).fetchOne();
        assertThat(rec).isNotNull();
        assertThat(rec.getDeletedAt()).isNotNull();
    }
}


