package org.certis.studyplatform.study.presentation;

import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.response.ResponseStatus;
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
@DisplayName("🧪 Admin Study Creation Approve/Reject Flow")
class AdminStudyCreationFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long memberId;
    private Long studyId;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
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

        // Insert study row
        studyId = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, memberId)
                .set(STUDY.TITLE, "승인 대상 스터디")
                .set(STUDY.DESCRIPTION, "설명")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(10))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);
    }

    @Test
    @DisplayName("관리자 생성 승인 - 200 OK")
    void approve_creation_ok() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        String body = "{\"studyId\": " + studyId + "}";
        mockMvc.perform(post("/api/v1/admin/study/create/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(STUDY).where(STUDY.ID.eq(studyId)).fetchOne();
        assertThat(row).isNotNull();
        // Approve creation now may leave status APPROVED but status string calculated as INPROGRESS
        // Repository updates status='APPROVED' but mapper derives INPROGRESS based on dates.
        // Here we assert started_at pulled to now to allow INPROGRESS calculation.
        assertThat(row.getStartedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    @DisplayName("관리자 생성 거절 - deleted_at 설정")
    void reject_creation_sets_deleted_at() throws Exception {
        var admin = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        String body = "{\"studyId\": " + studyId + "}";
        mockMvc.perform(post("/api/v1/admin/study/create/reject")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(STUDY).where(STUDY.ID.eq(studyId)).fetchOne();
        assertThat(row).isNotNull();
        assertThat(row.getDeletedAt()).isNotNull();
    }
}


