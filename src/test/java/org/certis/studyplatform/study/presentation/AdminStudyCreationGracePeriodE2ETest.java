package org.certis.studyplatform.study.presentation;

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
import static org.certis.generated.jooq.Tables.STUDY;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.test.annotation.DirtiesContext;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("🧪 Study creation approve extends grace period for UPSOLVER and reject sets deleted_at")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminStudyCreationGracePeriodE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long upsolverId;
    private Long staffId;
    private Long studyIdByUpsolver;
    private Long studyIdToReject;

    @BeforeEach
    void setUp() {
        truncateTableIfExists("study");
        truncateTableIfExists("member");

        OffsetDateTime now = OffsetDateTime.now();

        // UPSOLVER member
        upsolverId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "ups")
                .set(MEMBER.ROLE, "UPSOLVER")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240021")
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
                .set(MEMBER.STUDENT_NUMBER, "20240022")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.BIRTHDAY, now.minusYears(21))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);

        // Study created by UPSOLVER
        studyIdByUpsolver = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, upsolverId)
                .set(STUDY.TITLE, "S-UP")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.STARTED_AT, now.plusDays(2))
                .set(STUDY.ENDED_AT, now.plusDays(30))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);

        // Study to be rejected
        studyIdToReject = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, upsolverId)
                .set(STUDY.TITLE, "S-REJ")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.STARTED_AT, now.plusDays(3))
                .set(STUDY.ENDED_AT, now.plusDays(31))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);
    }

    private void truncateTableIfExists(String tableName) {
        try {
            Boolean exists = dsl.fetchOne(
                    "select exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = ?)",
                    tableName
            ).get(0, Boolean.class);
            if (Boolean.TRUE.equals(exists)) {
                dsl.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Creation approve extends member.grace_period for UPSOLVER")
    void approve_creation_extends_grace_period() throws Exception {
        var admin = new CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        var before = dsl.select(MEMBER.GRACE_PERIOD).from(MEMBER).where(MEMBER.ID.eq(upsolverId)).fetchOne(MEMBER.GRACE_PERIOD);
        assertThat(before).isNull();

        mockMvc.perform(post("/api/v1/admin/study/create/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyId\":" + studyIdByUpsolver + "}"))
                .andDo(print())
                .andExpect(status().isOk());

        var after = dsl.select(MEMBER.GRACE_PERIOD).from(MEMBER).where(MEMBER.ID.eq(upsolverId)).fetchOne(MEMBER.GRACE_PERIOD);
        assertThat(after).isNotNull();
        assertThat(after).isAfter(OffsetDateTime.now().minusMinutes(1));
    }

    @Test
    @DisplayName("Creation reject sets deleted_at on study")
    void reject_creation_sets_deleted_at() throws Exception {
        var admin = new CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        mockMvc.perform(post("/api/v1/admin/study/create/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studyId\":" + studyIdToReject + "}"))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.selectFrom(STUDY).where(STUDY.ID.eq(studyIdToReject)).fetchOne();
        assertThat(rec).isNotNull();
        assertThat(rec.getDeletedAt()).isNotNull();
    }
}


