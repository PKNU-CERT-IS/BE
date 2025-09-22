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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🌐 E2E: Study/Project S3 업로드 및 조회")
class StudyAndProjectS3E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private S3FileService s3FileService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_STUDY_ID = 2L;
    private static final Long TEST_PROJECT_ID = 10L;

    // 환경변수 로드 (.env 지원)
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")  // 프로젝트 루트의 .env 파일
            .ignoreIfMissing()
            .load();

    @BeforeEach
    void setUp() {
        dsl.execute("TRUNCATE TABLE study_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, "testuser")
                .set(MEMBER.STUDENT_NUMBER, "test@certis.org")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, now.minusYears(25))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .execute();

        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "S3 E2E Study")
                .set(STUDY.DESCRIPTION, "Study for S3 E2E")
                .set(STUDY.CONTENT, "Content")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발")
                .set(STUDY.SUBCATEGORY, "풀스택")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(30))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, "S3 E2E Project")
                .set(PROJECT.DESCRIPTION, "Project for S3 E2E")
                .set(PROJECT.CONTENT, "Project Content")
                .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT.CATEGORY, "웹 개발")
                .set(PROJECT.SUBCATEGORY, "풀스택")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(30))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();
    }

    @Test
    @DisplayName("Study 종료 API를 통해 파일 업로드 후 상세 조회에서 S3 URL 확인")
    void e2e_study_upload_and_detail_fetch_from_s3() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        MockMultipartFile file1 = new MockMultipartFile("files", "end-note.txt", "text/plain", "note".getBytes());
        MockMultipartFile image = new MockMultipartFile("files", "thumb.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/api/v1/study/end")
                        .file(file1)
                        .file(image)
                        .param("studyId", String.valueOf(TEST_STUDY_ID))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID));

        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments.length()") .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.thumbnailUrl").exists());
    }

    @Test
    @DisplayName("Project 상세 조회에서 S3 URL 형식이 반환되는지 확인")
    void e2e_project_detail_returns_s3_style_urls() throws Exception {
        // 첨부와 썸네일을 S3 URL 형식으로 직접 주입 (업로드 API 미존재)
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "guide.pdf")
                .set(PROJECT_ATTACHED.TYPE, "application/pdf")
                .set(PROJECT_ATTACHED.SIZE, "1234")
                .set(PROJECT_ATTACHED.ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/project-attachments/1/guide.pdf")
                .set(PROJECT_ATTACHED.CREATED_AT, now)
                .set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();

        mockMvc.perform(get("/api/v1/project/detail").param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.containsString("https://")));
    }
    // ===== 내부 유틸 =====
    private static String getenv(String key) {
        String v = dotenv.get(key);
        if (v == null || v.isEmpty()) {
            v = dotenv.get(key);
        }
        return v;
    }
}


