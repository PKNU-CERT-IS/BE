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
import org.certis.studyplatform.shared.security.CurrentUser;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.PROJECT;
import static org.certis.generated.jooq.Tables.MEMBER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("🧪 Admin Project Creation Approve/Reject Flow")
class AdminProjectCreationFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;
    @Autowired private ObjectMapper objectMapper;
    private Long memberId;
    private Long projectId;

    @BeforeEach
    void setUp() throws Exception {
        // 테이블이 존재하는 경우에만 TRUNCATE 실행
        truncateTableIfExists("project_participant");
        truncateTableIfExists("project");
        truncateTableIfExists("member");

        // Insert minimal member
        OffsetDateTime now = OffsetDateTime.now();
        memberId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "admin")
                .set(MEMBER.STUDENT_NUMBER, "20200001")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, now.minusYears(20))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);
        
        // Create via public API
        var admin = new CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");
        OffsetDateTime start = alignToNextMonday(now.plusDays(7)).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime end = alignToKstSunday(start.plusDays(7));
        String body = "{" +
                "\"title\":\"승인 대상 프로젝트\"," +
                "\"description\":\"설명\"," +
                "\"content\":\"내용\"," +
                "\"category\":\"CS\"," +
                "\"subCategory\":\"백엔드\"," +
                "\"startDate\":\"" + start.toString() + "\"," +
                "\"endDate\":\"" + end.toString() + "\"," +
                "\"maxParticipants\":5" +
                "}";

        mockMvc.perform(post("/api/v1/project/create")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated());

        // fetch created id
        var rec = dsl.selectFrom(PROJECT)
                .where(PROJECT.TITLE.eq("승인 대상 프로젝트"))
                .orderBy(PROJECT.ID.desc())
                .fetchOne();
        projectId = rec.getId();
    }

    @Test
    @DisplayName("관리자 생성 승인 - 200 OK")
    void approve_creation_ok() throws Exception {
        var admin = new CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

        String body = "{\"projectId\": " + projectId + "}";
        mockMvc.perform(post("/api/v1/admin/project/create/approve")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        var row = dsl.selectFrom(PROJECT).where(PROJECT.ID.eq(projectId)).fetchOne();
        assertThat(row).isNotNull();
        // After creation approve: persistence may not rewrite status immediately; just ensure not deleted
        assertThat(row.getDeletedAt()).isNull();

        // Verify read model computed status via detail endpoint
        var mvcResult = mockMvc.perform(get("/api/v1/project/detail").param("projectId", projectId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        String computedStatus = objectMapper.readTree(content).at("/data/status").asText();
        assertThat(computedStatus).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("관리자 생성 거절 - deleted_at 설정")
    void reject_creation_sets_deleted_at() throws Exception {
        var admin = new CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

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
        var admin = new CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");

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
        // End reject should clear attachments and set submission status rejected at domain level
        assertThat(row.getResultAttachedUrl()).isNull();
        assertThat(row.getResultSubmitStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("관리자 날짜 변경: INPROGRESS → startDate 미래로 이동 → APPROVED (read model)")
    void admin_date_change_inprogress_to_future_sets_approved_in_read_model() throws Exception {
        var admin = new CurrentUser(memberId, "admin", "admin@certis.org", "admin", "STAFF");
        // 1) Make it INPROGRESS via admin update: start in past, end in future
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStart = now.minusDays(1);
        OffsetDateTime futureEnd = now.plusDays(7);
        String makeInProgress = "{" +
                "\"projectId\":" + projectId + "," +
                "\"startDate\":\"" + pastStart.toString() + "\"," +
                "\"endDate\":\"" + futureEnd.toString() + "\"}";
        mockMvc.perform(put("/api/v1/admin/project/update")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(makeInProgress))
                .andDo(print())
                .andExpect(status().isOk());

        var detail1 = mockMvc.perform(get("/api/v1/project/detail").param("projectId", projectId.toString()))
                .andExpect(status().isOk()).andReturn();
        String status1 = objectMapper.readTree(detail1.getResponse().getContentAsString()).at("/data/status").asText();
        assertThat(status1).isEqualTo("INPROGRESS");

        // 2) Move startDate to future via admin update → read model should show APPROVED
        OffsetDateTime futureStart = alignToNextMonday(now.plusDays(14));
        String moveToApproved = "{" +
                "\"projectId\":" + projectId + "," +
                "\"startDate\":\"" + futureStart.toString() + "\"}";
        mockMvc.perform(put("/api/v1/admin/project/update")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveToApproved))
                .andDo(print())
                .andExpect(status().isOk());

        var detail2 = mockMvc.perform(get("/api/v1/project/detail").param("projectId", projectId.toString()))
                .andExpect(status().isOk()).andReturn();
        String status2 = objectMapper.readTree(detail2.getResponse().getContentAsString()).at("/data/status").asText();
        assertThat(status2).isEqualTo("APPROVED");
    }
    private static OffsetDateTime alignToNextMonday(OffsetDateTime source) {
        DayOfWeek dow = source.getDayOfWeek();
        int shift = DayOfWeek.MONDAY.getValue() - dow.getValue();
        if (shift < 0) shift += 7;
        return source.plusDays(shift);
    }

    private static OffsetDateTime alignToKstSunday(OffsetDateTime source) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        var zdt = source.atZoneSameInstant(kst);
        int shift = DayOfWeek.SUNDAY.getValue() - zdt.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        return zdt.plusDays(shift).withHour(23).withMinute(59).withSecond(59).withNano(0).toOffsetDateTime();
    }

    /**
     * 테이블이 존재하는 경우에만 TRUNCATE 실행
     */
    private void truncateTableIfExists(String tableName) {
        try {
            // 테이블 존재 여부 확인
            var result = dsl.select()
                    .from("information_schema.tables")
                    .where("table_name = ? AND table_schema = 'public'", tableName)
                    .fetch();
            
            if (!result.isEmpty()) {
                dsl.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
            }
        } catch (Exception e) {
            // 테이블이 존재하지 않거나 다른 오류가 발생한 경우 무시
            // 테스트에서는 테이블이 아직 생성되지 않았을 수 있음
        }
    }
}


