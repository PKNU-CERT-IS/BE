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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("🧪 Admin Study End Approve/Reject Flow")
class AdminStudyEndApproveRejectE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long staffId;
    private Long studyId;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        OffsetDateTime now = OffsetDateTime.now();

        staffId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "admin")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20250011")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.BIRTHDAY, now.minusYears(20))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);

        studyId = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, staffId)
                .set(STUDY.TITLE, "S-End")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(7))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);
    }

    @Test
    @DisplayName("End approve sets result_submit_status to COMPLETED")
    void end_approve_sets_completed() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        mockMvc.perform(post("/api/v1/admin/study/end/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("studyId", String.valueOf(studyId)))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("select result_submit_status from study where id=?", studyId);
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("End reject sets result_submit_status to REJECTED")
    void end_reject_sets_rejected() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(staffId, "admin", "a@b.c", "admin", "STAFF");

        dsl.update(STUDY)
                .set(STUDY.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(STUDY.RESULT_ATTACHED_URL, "https://bucket/key.txt")
                .where(STUDY.ID.eq(studyId))
                .execute();

        mockMvc.perform(post("/api/v1/admin/study/end/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("studyId", String.valueOf(studyId)))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("select result_submit_status from study where id=?", studyId);
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("REJECTED");
    }
}


