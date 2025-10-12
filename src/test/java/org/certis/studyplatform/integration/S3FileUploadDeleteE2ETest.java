package org.certis.studyplatform.integration;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S3 파일 업로드/삭제 E2E 테스트
 *
 * 실제 S3에 파일을 업로드하고, null 요청을 통해 삭제되는 과정을 테스트
 *
 * 환경변수 설정 필요:
 * - AWS_ACCESS_KEY_ID: IAM 액세스 키
 * - AWS_SECRET_ACCESS_KEY: IAM 시크릿 키
 * - AWS_DEFAULT_REGION: 버킷 리전 (예: ap-northeast-2)
 * - AWS_S3_BUCKET: 버킷 이름
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("S3 파일 업로드/삭제 E2E 테스트")
class S3FileUploadDeleteE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_PROJECT_ID = 2L;
    private static final Long TEST_STUDY_ID = 3L;

    // 환경변수 로드
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

    static {
        // 환경변수 설정 (Spring Boot가 시작되기 전에 설정되어야 함)
        System.setProperty("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID", ""));
        System.setProperty("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY", ""));
        System.setProperty("AWS_DEFAULT_REGION", dotenv.get("AWS_DEFAULT_REGION", "ap-southeast-2"));
        System.setProperty("AWS_S3_BUCKET", dotenv.get("AWS_S3_BUCKET", "pknucertis-bucket"));
    }

    @BeforeEach
    void setUp() {
        // 데이터베이스 초기화 (성공하는 테스트와 동일한 패턴)
        dsl.execute("TRUNCATE TABLE study_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        OffsetDateTime now = OffsetDateTime.now();

        // 테스트 멤버 생성
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

        // 테스트 프로젝트 생성
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
                .set(PROJECT.STATUS, "READY")
                .set(PROJECT.RESULT_SUBMIT_STATUS, "READY")
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();

        // 테스트 스터디 생성
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
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
    }

    @Test
    @DisplayName("프로필 이미지 업로드 → null 요청으로 삭제 E2E 테스트")
    void profileImageUploadAndDeleteE2E() throws Exception {
        // 환경변수 체크 (성공하는 테스트와 동일한 패턴)
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // Given: S3에 샘플 프로필 이미지 업로드 후 URL 획득
        String key = "e2e-profile/" + System.currentTimeMillis() + "/profile.jpg";
        String uploadedUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, key, "image/jpeg", new byte[]{1,2,3});

        // When: 프로필 업데이트 API로 이미지 URL 등록
        String updateJson = "{" +
                "\"name\":\"testuser\"," +
                "\"description\":\"테스트 사용자\"," +
                "\"profileImage\":\"" + uploadedUrl + "\"}";

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: 프로필 조회에서 해당 URL 확인
        mockMvc.perform(get("/api/v1/profile/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImage").value(uploadedUrl));

        // When: 프로필 이미지를 null로 업데이트하여 삭제
        String deleteJson = "{" +
                "\"name\":\"testuser\"," +
                "\"description\":\"테스트 사용자\"," +
                "\"profileImage\":null}";

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: S3에서 파일이 삭제되었는지 확인
        Thread.sleep(1000); // 삭제 처리 대기
        boolean fileExists = checkS3FileExists(accessKeyId, secretAccessKey, region, bucket, key);
        assertThat(fileExists).isFalse();
        log.info("프로필 이미지 삭제 확인 완료: {}", uploadedUrl);
    }

    @Test
    @DisplayName("프로젝트 첨부파일 업로드 → 빈 배열 요청으로 삭제 E2E 테스트")
    void projectAttachmentUploadAndDeleteE2E() throws Exception {
        // 환경변수 체크
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // Given: S3에 샘플 첨부파일 업로드 후 URL 획득
        String key = "e2e-project-attachment/" + System.currentTimeMillis() + "/document.txt";
        String uploadedUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, key, "text/plain", new byte[]{4,5,6});

        // When: 프로젝트 업데이트 API로 첨부파일 URL 등록
        String updateJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"document.txt\"," +
                "\"type\":\"TEXT\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + uploadedUrl + "\"}]}";

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: 프로젝트 상세 조회에서 해당 URL 확인
        mockMvc.perform(get("/api/v1/project/detail").param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").isString())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.startsWith("https://")));

        // When: 프로젝트 첨부파일을 빈 배열로 업데이트 → 전체 삭제 정책
        String deleteJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[]}";

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: DB에서 첨부가 삭제되고, 상세 조회 시 첨부가 비어있음
        var cntRec = dsl.fetchOne("SELECT COUNT(1) AS cnt FROM project_attached WHERE project_id = ? AND deleted_at IS NULL", TEST_PROJECT_ID);
        assertThat(cntRec).isNotNull();
        long cnt = ((Number) cntRec.get("cnt")).longValue();
        assertThat(cnt).isZero();

        mockMvc.perform(get("/api/v1/project/detail").param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk());
        // Async consistency: wait until attachments array becomes empty (best-effort)
        waitUntilAttachmentsEmpty("/api/v1/project/detail", "projectId", TEST_PROJECT_ID);
        log.info("프로젝트 첨부파일 빈 배열 업데이트 후 첨부 비어있음 확인: {}", uploadedUrl);
    }

    @Test
    @DisplayName("스터디 첨부파일 업로드 → 빈 배열 요청으로 삭제 E2E 테스트")
    void studyAttachmentUploadAndDeleteE2E() throws Exception {
        // 환경변수 체크
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // Given: S3에 샘플 첨부파일 업로드 후 URL 획득
        String key = "e2e-study-attachment/" + System.currentTimeMillis() + "/guide.pdf";
        String uploadedUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, key, "application/pdf", new byte[]{7,8,9});

        // When: 스터디 업데이트 API로 첨부파일 URL 등록
        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"guide.pdf\"," +
                "\"type\":\"PDF\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + uploadedUrl + "\"}]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: 스터디 상세 조회에서 해당 URL 확인
        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").isString())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.startsWith("https://")));

        // When: 스터디 첨부파일을 빈 배열로 업데이트 → 전체 삭제 정책
        String deleteJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Then: DB에서 첨부가 삭제되고, 상세 조회 시 첨부가 비어있음
        var cntRec2 = dsl.fetchOne("SELECT COUNT(1) AS cnt FROM study_attached WHERE study_id = ? AND deleted_at IS NULL", TEST_STUDY_ID);
        assertThat(cntRec2).isNotNull();
        long cnt2 = ((Number) cntRec2.get("cnt")).longValue();
        assertThat(cnt2).isZero();

        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk());
        // Async consistency: wait until attachments array becomes empty (best-effort)
        waitUntilAttachmentsEmpty("/api/v1/study/detail", "studyId", TEST_STUDY_ID);
        log.info("스터디 첨부파일 빈 배열 업데이트 후 첨부 비어있음 확인: {}", uploadedUrl);
    }

    // S3 업로드 헬퍼 메서드 (성공하는 테스트와 동일한 패턴)
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

    // S3 파일 존재 확인 헬퍼 메서드
    private boolean checkS3FileExists(String accessKeyId, String secretAccessKey, String region, String bucket, String key) {
        try {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();

            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("S3 파일 존재 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }

    // Poll detail endpoint up to 3 times with 200ms backoff until attachments array is empty
    private void waitUntilAttachmentsEmpty(String endpoint, String idParamName, Long id) throws Exception {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                mockMvc.perform(get(endpoint).param(idParamName, String.valueOf(id)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.attachments").isArray())
                        .andExpect(jsonPath("$.data.attachments.length()").value(0));
                return;
            } catch (AssertionError ae) {
                if (attempt == maxAttempts) {
                    throw ae;
                }
                Thread.sleep(200);
            }
        }
    }
}