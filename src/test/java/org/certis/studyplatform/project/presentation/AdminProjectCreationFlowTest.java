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
@DisplayName("🧪 Admin Project Creation Approve/Reject Flow")
class AdminProjectCreationFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long memberId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        // Insert minimal member
        OffsetDateTime now = OffsetDateTime.now();
        memberId = dsl.insertInto(org.certis.generated.jooq.Tables.MEMBER)
                .set(org.certis.generated.jooq.Tables.MEMBER.NAME, "admin")
                .set(org.certis.generated.jooq.Tables.MEMBER.STUDENT_NUMBER, "20200001")
                .set(org.certis.generated.jooq.Tables.MEMBER.ROLE, "STAFF")
                .set(org.certis.generated.jooq.Tables.MEMBER.GRADE, "SENIOR")
                .set(org.certis.generated.jooq.Tables.MEMBER.MAJOR, "컴퓨터공학과")
                .set(org.certis.generated.jooq.Tables.MEMBER.BIRTHDAY, now.minusYears(20))
                .set(org.certis.generated.jooq.Tables.MEMBER.GENDER, "M")
                .set(org.certis.generated.jooq.Tables.MEMBER.CREATED_AT, now)
                .set(org.certis.generated.jooq.Tables.MEMBER.UPDATED_AT, now)
                .returning(org.certis.generated.jooq.Tables.MEMBER.ID)
                .fetchOne()
                .get(org.certis.generated.jooq.Tables.MEMBER.ID);

        // Insert project row
        projectId = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, memberId)
                .set(PROJECT.TITLE, "승인 대상 프로젝트")
                .set(PROJECT.DESCRIPTION, "설명")
                .set(PROJECT.CONTENT, "내용")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "백엔드")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STATUS, "READY")
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(10))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .returning(PROJECT.ID)
                .fetchOne()
                .get(PROJECT.ID);
    }

    @Test
    @DisplayName("관리자 생성 승인 - 200 OK")
    void approve_creation_ok() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        String body = "{\"projectId\": " + projectId + "}";
        mockMvc.perform(post("/api/v1/admin/project/create/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(PROJECT).where(PROJECT.ID.eq(projectId)).fetchOne();
        assertThat(row).isNotNull();
        // Approve creation now moves status to INPROGRESS and may pull started_at to now
        assertThat(row.getStatus()).isEqualTo("INPROGRESS");
        assertThat(row.getStartedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    @DisplayName("관리자 생성 거절 - deleted_at 설정")
    void reject_creation_sets_deleted_at() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        String body = "{\"projectId\": " + projectId + "}";
        mockMvc.perform(post("/api/v1/admin/project/create/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(PROJECT).where(PROJECT.ID.eq(projectId)).fetchOne();
        assertThat(row).isNotNull();
        assertThat(row.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자 종료 거절 - REJECTED 되나 deleted_at은 유지(null)")
    void reject_end_sets_rejected_and_keeps_deleted_at_null() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        // prepare project as if end submission is in progress
        OffsetDateTime now = OffsetDateTime.now();
        dsl.update(PROJECT)
                .set(PROJECT.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(PROJECT.RESULT_SUBMITTED_AT, now.minusHours(1))
                .set(PROJECT.RESULT_ATTACHED_URL, "https://bucket/obj.pdf")
                .where(PROJECT.ID.eq(projectId))
                .execute();

        String body = "{\"projectId\": " + projectId + "}";
        mockMvc.perform(post("/api/v1/admin/project/end/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(PROJECT).where(PROJECT.ID.eq(projectId)).fetchOne();
        assertThat(row).isNotNull();
        assertThat(row.getStatus()).isEqualTo("REJECTED");
        assertThat(row.getDeletedAt()).isNull();
    }
}


