package org.certis.studyplatform.project.presentation;

import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🚀 Admin Project End Flow E2E Test")
class AdminProjectEndFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    private static final String ADMIN_BASE = "/api/v1/admin/project";
    private static final Long ADMIN_ID = 3L; // staff
    private static final Long CREATOR_ID = 3L;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        // admin member
        dsl.execute("INSERT INTO member(id, name, role, grade, student_number, major, birthday, gender, created_at, updated_at) " +
                "VALUES (?, '관리자', 'STAFF', 'SENIOR', '20240001', 'CS', now() - interval '20 years', 'MALE', now(), now())", ADMIN_ID);

        // project
        dsl.execute("INSERT INTO project(id, member_id, title, description, content, category, subcategory, started_at, ended_at, max_participants_number, created_at, updated_at) " +
                "VALUES (?, ?, 'P1', 'desc', 'content', 'CS', 'BE', now() + interval '1 day', now() + interval '60 days', 5, now(), now())",
                PROJECT_ID, CREATOR_ID);
    }

    @AfterEach
    void tearDown() {
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("관리자가 종료 승인 시 상태가 COMPLETED 되고 ended_at이 갱신된다")
    void approve_end_updates_status_and_ended_at() throws Exception {
        mockMvc.perform(post(ADMIN_BASE + "/end/approve")
                        .param("projectId", String.valueOf(PROJECT_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("select result_submit_status, ended_at from project where id = ?", PROJECT_ID);
        assertThat(rec).isNotNull();
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("COMPLETED");
        assertThat(rec.get("ended_at", java.time.OffsetDateTime.class)).isNotNull();
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("관리자가 종료 거절 시 상태가 REJECTED 되고 첨부/제출시간이 초기화된다")
    void reject_end_clears_submission_fields() throws Exception {
        // seed submission fields
        dsl.execute("update project set result_submit_status='INPROGRESS', result_submitted_at=now(), result_attached_url='https://bucket/file' where id=?", PROJECT_ID);

        mockMvc.perform(post(ADMIN_BASE + "/end/reject")
                        .param("projectId", String.valueOf(PROJECT_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("select result_submit_status, result_attached_url, result_submitted_at from project where id = ?", PROJECT_ID);
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("REJECTED");
        assertThat(rec.get("result_attached_url", String.class)).isNull();
        assertThat(rec.get("result_submitted_at", java.time.OffsetDateTime.class)).isNull();
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("관리자가 종료 제출 조회 시 상태/시간/첨부가 반환된다")
    void get_submission_returns_payload() throws Exception {
        dsl.execute("update project set result_submit_status='INPROGRESS', result_submitted_at=now(), result_attached_url='https://bucket/file' where id=?", PROJECT_ID);

        mockMvc.perform(get(ADMIN_BASE + "/end/" + PROJECT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INPROGRESS"))
                .andExpect(jsonPath("$.data.attachments").isNotEmpty())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID));
    }
}


