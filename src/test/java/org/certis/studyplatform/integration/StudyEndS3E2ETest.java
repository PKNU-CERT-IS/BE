package org.certis.studyplatform.integration;
import io.github.cdimascio.dotenv.Dotenv;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.shared.service.S3FileService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("🚀 Study End S3 E2E Test (real S3 if creds present)")
class StudyEndS3E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private S3FileService s3FileService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_BASE = "/api/v1/study";
    private static final Long STAFF_ID = 3L;
    private static final Long STUDY_ID = 1L;

    // .env 로드 및 시스템 프로퍼티 주입 (ProjectEndS3E2ETest와 동일 패턴)
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

    static {
        System.setProperty("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID", System.getProperty("AWS_ACCESS_KEY_ID", "")));
        System.setProperty("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY", System.getProperty("AWS_SECRET_ACCESS_KEY", "")));
        System.setProperty("AWS_DEFAULT_REGION", dotenv.get("AWS_DEFAULT_REGION", System.getProperty("AWS_DEFAULT_REGION", "ap-southeast-2")));
        System.setProperty("AWS_S3_BUCKET", dotenv.get("AWS_S3_BUCKET", System.getProperty("AWS_S3_BUCKET", "pknucertis-bucket")));
    }

    @BeforeEach
    void setUp() {
        truncateTableIfExists("study");
        truncateTableIfExists("member");

        // admin/staff
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, STAFF_ID)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240001")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.BIRTHDAY, java.time.OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, java.time.OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();

        // study
        dsl.insertInto(STUDY)
                .set(STUDY.ID, STUDY_ID)
                .set(STUDY.TITLE, "S1")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(STUDY.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(STUDY.MEMBER_ID, STAFF_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();
    }

    @AfterEach
    void tearDown() {
        truncateTableIfExists("study");
        truncateTableIfExists("member");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/study/end uploads data URL to real S3 and stores URL when creds exist")
    void end_uploads_data_url_to_s3_and_stores_url() throws Exception {
        boolean hasAccessKey = notEmpty(System.getProperty("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID")));
        boolean hasSecretKey = notEmpty(System.getProperty("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY")));
        boolean hasRegion = notEmpty(System.getProperty("AWS_DEFAULT_REGION", System.getenv("AWS_DEFAULT_REGION")));
        boolean hasBucket = notEmpty(System.getProperty("AWS_S3_BUCKET", System.getenv("AWS_S3_BUCKET")));
        boolean hasCredentials = hasAccessKey && hasSecretKey && hasRegion && hasBucket;

        // Create a data URL with base64 encoded content
        String fileContent = "hello world from study end test";
        String base64Content = java.util.Base64.getEncoder().encodeToString(fileContent.getBytes());
        String dataUrl = "data:text/plain;base64," + base64Content;

        // Create request DTO
        var requestDto = new java.util.HashMap<String, Object>();
        requestDto.put("studyId", STUDY_ID);
        requestDto.put("attachment", Map.of(
                "attachedUrl", dataUrl,
                "name", "report.pdf",
                "type", "application/pdf",
                "size", "1234"
        ));

        if (hasCredentials) {
            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk());

            var rec = dsl.fetchOne("select result_submit_status, result_attached_url from study where id=?", STUDY_ID);
            assertThat(rec.get("result_submit_status", String.class)).isEqualTo("INPROGRESS");
            String url = rec.get("result_attached_url", String.class);
            assertThat(url).isNotBlank();
            assertThat(url).startsWith("https://");
            assertThat(s3FileService.fileExists(url)).isTrue();
        } else {
            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        }
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/study/end accepts existing S3 URL without re-uploading")
    void end_accepts_existing_s3_url() throws Exception {
        boolean hasAccessKey = notEmpty(System.getProperty("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID")));
        boolean hasSecretKey = notEmpty(System.getProperty("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY")));
        boolean hasRegion = notEmpty(System.getProperty("AWS_DEFAULT_REGION", System.getenv("AWS_DEFAULT_REGION")));
        boolean hasBucket = notEmpty(System.getProperty("AWS_S3_BUCKET", System.getenv("AWS_S3_BUCKET")));
        boolean hasCredentials = hasAccessKey && hasSecretKey && hasRegion && hasBucket;

        // Create a new study for this test to avoid conflicts
        Long testStudyId = 2L;
        dsl.insertInto(STUDY)
                .set(STUDY.ID, testStudyId)
                .set(STUDY.TITLE, "S2")
                .set(STUDY.DESCRIPTION, "desc2")
                .set(STUDY.CONTENT, "content2")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(STUDY.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(STUDY.MEMBER_ID, STAFF_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();

        if (hasCredentials) {
            // First upload a file to get a real S3 URL
            String fileContent = "existing file content";
            String base64Content = java.util.Base64.getEncoder().encodeToString(fileContent.getBytes());
            String dataUrl = "data:text/plain;base64," + base64Content;

            var requestDto1 = new java.util.HashMap<String, Object>();
            requestDto1.put("studyId", testStudyId);
            requestDto1.put("attachment", Map.of(
                    "attachedUrl", dataUrl,
                    "name", "report.pdf",
                    "type", "application/pdf",
                    "size", "1234"
            ));

            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto1)))
                    .andDo(print())
                    .andExpect(status().isOk());

            // Get the S3 URL from the first upload
            var rec1 = dsl.fetchOne("select result_attached_url from study where id=?", testStudyId);
            String existingS3Url = rec1.get("result_attached_url", String.class);

            // Now test with the existing S3 URL (this should fail because study is already ended)
            var requestDto2 = new java.util.HashMap<String, Object>();
            requestDto2.put("studyId", testStudyId);
            requestDto2.put("attachment", Map.of(
                    "attachedUrl", existingS3Url,
                    "name", "report.pdf",
                    "type", "application/pdf",
                    "size", "1234"
            ));

            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto2)))
                    .andDo(print())
                    .andExpect(status().isUnprocessableEntity()); // Should fail because study is already ended

            // Verify the study is in INPROGRESS status
            var rec2 = dsl.fetchOne("select result_submit_status, result_attached_url from study where id=?", testStudyId);
            assertThat(rec2.get("result_submit_status", String.class)).isEqualTo("INPROGRESS");
            String finalUrl = rec2.get("result_attached_url", String.class);
            assertThat(finalUrl).isEqualTo(existingS3Url);
        } else {
            // Test with a mock S3 URL when credentials are not available
            String mockS3Url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt";
            
            var requestDto = new java.util.HashMap<String, Object>();
            requestDto.put("studyId", testStudyId);
            requestDto.put("attachment", Map.of(
                    "attachedUrl", mockS3Url,
                    "name", "report.pdf",
                    "type", "application/pdf",
                    "size", "1234"
            ));

            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk());

            var rec = dsl.fetchOne("select result_submit_status, result_attached_url from study where id=?", testStudyId);
            assertThat(rec.get("result_submit_status", String.class)).isEqualTo("INPROGRESS");
            String url = rec.get("result_attached_url", String.class);
            assertThat(url).isEqualTo(mockS3Url);
        }
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Admin can list pending study end submissions")
    void admin_can_list_pending_study_end_submissions() throws Exception {
        // Create a study with INPROGRESS status
        dsl.update(STUDY)
                .set(STUDY.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(STUDY.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt")
                .where(STUDY.ID.eq(STUDY_ID))
                .execute();

        mockMvc.perform(get("/api/v1/admin/study/end"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Admin can get specific study end submission details")
    void admin_can_get_study_end_submission_details() throws Exception {
        // Create a study with INPROGRESS status
        dsl.update(STUDY)
                .set(STUDY.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(STUDY.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-file.txt")
                .where(STUDY.ID.eq(STUDY_ID))
                .execute();

        mockMvc.perform(get("/api/v1/admin/study/end/" + STUDY_ID))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Admin /end endpoint returns only INPROGRESS studies, filters out other statuses")
    void admin_end_endpoint_filters_inprogress_studies() throws Exception {
        // Create multiple studies with different statuses
        Long studyId2 = 2L;
        Long studyId3 = 3L;
        Long studyId4 = 4L;

        // Study 1: INPROGRESS (should be returned)
        dsl.update(STUDY)
                .set(STUDY.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(STUDY.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusHours(1))
                .set(STUDY.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/study1.txt")
                .where(STUDY.ID.eq(STUDY_ID))
                .execute();

        // Study 2: INPROGRESS (should be returned)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId2)
                .set(STUDY.TITLE, "S2")
                .set(STUDY.DESCRIPTION, "desc2")
                .set(STUDY.CONTENT, "content2")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(STUDY.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(STUDY.MEMBER_ID, STAFF_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(STUDY.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusMinutes(30))
                .set(STUDY.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/study2.txt")
                .execute();

        // Study 3: COMPLETED (should NOT be returned)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId3)
                .set(STUDY.TITLE, "S3")
                .set(STUDY.DESCRIPTION, "desc3")
                .set(STUDY.CONTENT, "content3")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(STUDY.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(STUDY.MEMBER_ID, STAFF_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.RESULT_SUBMIT_STATUS, "COMPLETED")
                .set(STUDY.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusDays(1))
                .set(STUDY.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/study3.txt")
                .execute();

        // Study 4: NULL status (should NOT be returned)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId4)
                .set(STUDY.TITLE, "S4")
                .set(STUDY.DESCRIPTION, "desc4")
                .set(STUDY.CONTENT, "content4")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(STUDY.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(STUDY.MEMBER_ID, STAFF_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .setNull(STUDY.RESULT_SUBMITTED_AT)
                .setNull(STUDY.RESULT_ATTACHED_URL)
                .execute();

        // Test the admin endpoint
        var result = mockMvc.perform(get("/api/v1/admin/study/end"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response to verify only INPROGRESS studies are returned
        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        var data = response.get("data");
        
        // Should return INPROGRESS studies (at least the two we set)
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
        
        // Verify the returned studies have INPROGRESS resultSubmitStatus
        var studyIds = new java.util.HashSet<Long>();
        for (var study : data) {
            Long id = study.get("studyId").asLong();
            String resultStatus = study.get("resultSubmitStatus").asText();
            studyIds.add(id);
            assertThat(resultStatus).isEqualTo("INPROGRESS");
        }
        
        // Verify known INPROGRESS studies are included
        assertThat(studyIds).contains(STUDY_ID, studyId2);
    }

    private boolean notEmpty(String v) {
        return v != null && !v.isEmpty();
    }

    private void truncateTableIfExists(String tableName) {
        try {
            dsl.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
        } catch (Exception ignored) {
        }
    }
}


