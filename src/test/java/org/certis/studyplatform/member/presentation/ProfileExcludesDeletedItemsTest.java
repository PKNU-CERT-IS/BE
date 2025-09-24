package org.certis.studyplatform.member.presentation;

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
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("👤 profile/me excludes soft-deleted studies and projects")
class ProfileExcludesDeletedItemsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    private Long memberId;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE study_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        OffsetDateTime now = OffsetDateTime.now();
        memberId = dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, "me")
                .set(MEMBER.STUDENT_NUMBER, "20200001")
                .set(MEMBER.ROLE, "UPSOLVER")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, now.minusYears(20))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .returning(MEMBER.ID)
                .fetchOne()
                .get(MEMBER.ID);

        // Study: one visible, one soft-deleted
        Long s1 = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, memberId)
                .set(STUDY.TITLE, "보이는 스터디")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(10))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .returning(STUDY.ID)
                .fetchOne().get(STUDY.ID);
        Long s2 = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, memberId)
                .set(STUDY.TITLE, "삭제된 스터디")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(10))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .set(STUDY.DELETED_AT, now)
                .returning(STUDY.ID)
                .fetchOne().get(STUDY.ID);

        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.STUDY_ID, s1)
                .set(STUDY_PARTICIPANT.MEMBER_ID, memberId)
                .set(STUDY_PARTICIPANT.STATUS, org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();
        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.STUDY_ID, s2)
                .set(STUDY_PARTICIPANT.MEMBER_ID, memberId)
                .set(STUDY_PARTICIPANT.STATUS, org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();

        // Project: one visible, one soft-deleted
        Long p1 = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, memberId)
                .set(PROJECT.TITLE, "보이는 프로젝트")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(10))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .returning(PROJECT.ID)
                .fetchOne().get(PROJECT.ID);
        Long p2 = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, memberId)
                .set(PROJECT.TITLE, "삭제된 프로젝트")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(10))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .set(PROJECT.DELETED_AT, now)
                .returning(PROJECT.ID)
                .fetchOne().get(PROJECT.ID);

        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, p1)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, now)
                .set(PROJECT_PARTICIPANT.UPDATED_AT, now)
                .execute();
        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, p2)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, now)
                .set(PROJECT_PARTICIPANT.UPDATED_AT, now)
                .execute();
    }

    @Test
    void profile_me_study_should_exclude_deleted() throws Exception {
        var me = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "me", "me@certis.org", "me", "UPSOLVER");
        var result = mockMvc.perform(get("/api/v1/profile/me/study").with(user(me)).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        com.fasterxml.jackson.databind.JsonNode data = root.path("data");
        for (com.fasterxml.jackson.databind.JsonNode node : data) {
            assertThat(node.path("title").asText()).isNotEqualTo("삭제된 스터디");
        }
    }

    @Test
    void profile_me_project_should_exclude_deleted() throws Exception {
        var me = new org.certis.studyplatform.shared.security.CurrentUser(memberId, "me", "me@certis.org", "me", "UPSOLVER");
        var result = mockMvc.perform(get("/api/v1/profile/me/project").with(user(me)).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        com.fasterxml.jackson.databind.JsonNode data = root.path("data");
        for (com.fasterxml.jackson.databind.JsonNode node : data) {
            assertThat(node.path("title").asText()).isNotEqualTo("삭제된 프로젝트");
        }
    }
}


