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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.STUDY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("🔎 study/search excludes soft-deleted rows")
class StudySearchExcludesDeletedTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DSLContext dsl;

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        OffsetDateTime now = OffsetDateTime.now();

        // Visible row (not deleted)
        dsl.insertInto(STUDY)
                .set(STUDY.TITLE, "보이는 스터디")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(10))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // Soft-deleted row
        dsl.insertInto(STUDY)
                .set(STUDY.TITLE, "삭제된 스터디")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(10))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .set(STUDY.DELETED_AT, now)
                .execute();
    }

    @Test
    void search_should_exclude_deleted() throws Exception {
        var result = mockMvc.perform(get("/api/v1/study/search").param("keyword", "스터디"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        com.fasterxml.jackson.databind.JsonNode content = root.path("data").path("content");
        assertThat(content.isArray()).isTrue();
        for (com.fasterxml.jackson.databind.JsonNode node : content) {
            assertThat(node.path("title").asText()).isNotEqualTo("삭제된 스터디");
        }
    }
}


