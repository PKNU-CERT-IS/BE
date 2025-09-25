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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🚀 Study End S3 E2E Test (real S3 if creds present)")
class StudyEndS3E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private S3FileService s3FileService;

    

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
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

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
                .set(STUDY.CREATED_AT, java.time.OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();
    }

    @AfterEach
    void tearDown() {
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/study/end uploads file to real S3 and stores single URL when creds exist")
    void end_uploads_to_s3_and_stores_urls() throws Exception {
        boolean hasAccessKey = notEmpty(System.getProperty("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID")));
        boolean hasSecretKey = notEmpty(System.getProperty("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY")));
        boolean hasRegion = notEmpty(System.getProperty("AWS_DEFAULT_REGION", System.getenv("AWS_DEFAULT_REGION")));
        boolean hasBucket = notEmpty(System.getProperty("AWS_S3_BUCKET", System.getenv("AWS_S3_BUCKET")));
        boolean hasCredentials = hasAccessKey && hasSecretKey && hasRegion && hasBucket;

        MockMultipartFile file1 = new MockMultipartFile("attachment", "report1.txt", MediaType.TEXT_PLAIN_VALUE,
                "hello world".getBytes());

        if (hasCredentials) {
            mockMvc.perform(multipart(USER_BASE + "/end")
                            .file("attachment", file1.getBytes())
                            .param("studyId", String.valueOf(STUDY_ID)))
                    .andDo(print())
                    .andExpect(status().isOk());

            var rec = dsl.fetchOne("select result_submit_status, result_attached_url from study where id=?", STUDY_ID);
            assertThat(rec.get("result_submit_status", String.class)).isEqualTo("COMPLETED");
            String url = rec.get("result_attached_url", String.class);
            assertThat(url).isNotBlank();
            assertThat(url).startsWith("https://");
            assertThat(s3FileService.fileExists(url)).isTrue();
        } else {
            mockMvc.perform(multipart(USER_BASE + "/end")
                            .file("attachment", file1.getBytes())
                            .param("studyId", String.valueOf(STUDY_ID)))
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        }
    }

    private boolean notEmpty(String v) {
        return v != null && !v.isEmpty();
    }
}


