package org.certis.studyplatform.integration;

import io.github.cdimascio.dotenv.Dotenv;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.shared.service.S3FileService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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

    @Test
    @DisplayName("Study 업데이트로 첨부파일(S3 URL) 추가 후 상세 조회로 검증")
    void e2e_study_update_add_attachments_and_verify() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // Given: S3에 샘플 파일 업로드 후 URL 획득
        String key = "e2e-study-update/" + System.currentTimeMillis() + "/thumb.jpg";
        String uploadedUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, key, "image/jpeg", new byte[]{1,2,3});

        // When: 업데이트 API로 첨부파일 URL 등록 (MIME 타입 대신 enum 값 사용)
        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"thumb.jpg\"," +
                "\"type\":\"JPEG\"," +  // image/jpeg → JPEG로 변경
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + uploadedUrl + "\"}]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: 상세 조회에서 해당 URL과 thumbnailUrl 확인
        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(uploadedUrl))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(uploadedUrl));
    }

    @Test
    @DisplayName("Project 업데이트로 첨부파일(S3 URL) 추가 후 상세 조회로 검증")
    void e2e_project_update_add_attachments_and_verify() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // Given: S3에 샘플 파일 업로드 후 URL 획득
        String key = "e2e-project-update/" + System.currentTimeMillis() + "/spec.pdf";
        String uploadedUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, key, "application/pdf", new byte[]{4,5,6});

        // When: 업데이트 API로 첨부파일 URL 등록 (MIME 타입 대신 enum 값 사용)
        String updateJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"spec.pdf\"," +
                "\"type\":\"PDF\"," +  // application/pdf → PDF로 변경
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + uploadedUrl + "\"}]}";

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: 상세 조회에서 해당 URL 확인
        mockMvc.perform(get("/api/v1/project/detail").param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(uploadedUrl));
    }

    private String uploadToS3(String accessKeyId, String secretAccessKey, String region, String bucket, String key,
                              String contentType, byte[] data) {
        S3Client s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3.putObject(put, RequestBody.fromBytes(data));
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}


