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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.certis.studyplatform.shared.security.CurrentUser;

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

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_STUDY_ID = 2L;
    private static final Long TEST_PROJECT_ID = 10L;

    // 환경변수 로드 (.env 지원)
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")  // 프로젝트 루트의 .env 파일
            .ignoreIfMissing()
            .load();

        static {
        // Spring Context 로딩 전에 시스템 프로퍼티로 주입
        System.setProperty("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID", ""));
        System.setProperty("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY", ""));
        System.setProperty("AWS_DEFAULT_REGION", dotenv.get("AWS_DEFAULT_REGION", "ap-southeast-2"));
        System.setProperty("AWS_S3_BUCKET", dotenv.get("AWS_S3_BUCKET", "pknucertis-bucket"));
        }

    @BeforeEach
    void setUp() {
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.execute("TRUNCATE TABLE study_participant RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE study_attached RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE project_attached RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
            tx.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

            OffsetDateTime now = OffsetDateTime.now();
            tx.insertInto(MEMBER)
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

            tx.insertInto(STUDY)
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

            tx.insertInto(PROJECT)
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
        });
    }

    private void setupAuthentication(Long memberId, String memberName) {
        CurrentUser currentUser = new CurrentUser(
                memberId,
                "testuser" + memberId,
                "test" + memberId + "@certis.org",
                memberName,
                "MEMBER"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
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

    @Test
    @DisplayName("Study 업데이트 시 이미지 첨부가 있으면 thumbnailUrl이 해당 S3 URL로 설정된다")
    void e2e_study_update_sets_thumbnail_from_image_url() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        String imgKey = "e2e-study-thumb/" + System.currentTimeMillis() + "/cover.jpg";
        String imgUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, imgKey, "image/jpeg", new byte[]{7,8,9});

        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"cover.jpg\"," +
                "\"type\":\"JPEG\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + imgUrl + "\"}]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thumbnailUrl").value(imgUrl));
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

    private void applyAwsSystemProperties(String accessKeyId, String secretAccessKey, String region) {
        System.setProperty("aws.accessKeyId", accessKeyId);
        System.setProperty("aws.secretAccessKey", secretAccessKey);
        if (region != null && !region.isEmpty()) {
            System.setProperty("aws.region", region);
        }
    }

    private void assumeS3Accessible(String accessKeyId, String secretAccessKey, String region, String bucket) {
        try {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();
            s3.headBucket(b -> b.bucket(bucket));
        } catch (Exception ex) {
            assumeTrue(false, "S3 접근이 불가능합니다: " + ex.getMessage());
        }
    }

    @Test
    @DisplayName("블로그 참조 목록 - 스터디 종료 후 referenceId/referenceTitle 포함 (실제 S3 업로드 경유)")
    void e2e_study_end_then_blog_reference_contains_studyWithReferenceFields() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        assumeS3Accessible(accessKeyId, secretAccessKey, region, bucket);
        applyAwsSystemProperties(accessKeyId, secretAccessKey, region);

        setupAuthentication(TEST_MEMBER_ID, "testuser");

        // 종료 제출에 필요한 첨부 파일 (실제 업로드는 서비스가 수행)
        MockMultipartFile dummy = new MockMultipartFile("attachment", "end.txt", "text/plain", "done".getBytes());

        // 사전: 생성자 본인 참가 승인
        OffsetDateTime pre = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(STUDY_PARTICIPANT)
                    .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY_PARTICIPANT.STATUS, "APPROVED")
                    .set(STUDY_PARTICIPANT.CREATED_AT, pre)
                    .set(STUDY_PARTICIPANT.UPDATED_AT, pre)
                    .execute();
        });

        mockMvc.perform(multipart("/api/v1/study/end")
                        .file(dummy)
                        .param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 생성자 본인도 참가자로 승인 처리 (참조 기준)
        OffsetDateTime now = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(STUDY_PARTICIPANT)
                    .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY_PARTICIPANT.STATUS, "APPROVED")
                    .set(STUDY_PARTICIPANT.CREATED_AT, now)
                    .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                    .execute();
        });

        var mvcResult = mockMvc.perform(get("/api/v1/blog/reference"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        var dataArray = objectMapper.readTree(body).get("data");
        boolean found = false;
        if (dataArray != null && dataArray.isArray()) {
            for (var node : dataArray) {
                if (node.hasNonNull("referenceId")) {
                    long id = node.get("referenceId").asLong();
                    if (id == TEST_STUDY_ID) {
                        found = true;
                        break;
                    }
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(found).isTrue();
    }

    @Test
    @DisplayName("블로그 참조 목록 - 프로젝트 종료 후 referenceId/referenceTitle 포함 (실제 S3 업로드 경유)")
    void e2e_project_end_then_blog_reference_contains_projectWithReferenceFields() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucket != null && !bucket.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        assumeS3Accessible(accessKeyId, secretAccessKey, region, bucket);
        applyAwsSystemProperties(accessKeyId, secretAccessKey, region);

        setupAuthentication(TEST_MEMBER_ID, "testuser");

        MockMultipartFile dummy = new MockMultipartFile("attachment", "end.txt", "text/plain", "done".getBytes());
        // 사전: 생성자 본인 참가 승인
        OffsetDateTime pre = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(PROJECT_PARTICIPANT)
                    .set(PROJECT_PARTICIPANT.PROJECT_ID, TEST_PROJECT_ID)
                    .set(PROJECT_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT_PARTICIPANT.STATUS, "APPROVED")
                    .set(PROJECT_PARTICIPANT.CREATED_AT, pre)
                    .set(PROJECT_PARTICIPANT.UPDATED_AT, pre)
                    .execute();
        });
        mockMvc.perform(multipart("/api/v1/project/end")
                        .file(dummy)
                        .param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 종료 직후 ended_at 과거 보정은 내부 승인 시점으로 처리되므로 생략

        // 생성자 본인도 참가자로 승인 처리
        OffsetDateTime now = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(PROJECT_PARTICIPANT)
                    .set(PROJECT_PARTICIPANT.PROJECT_ID, TEST_PROJECT_ID)
                    .set(PROJECT_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT_PARTICIPANT.STATUS, "APPROVED")
                    .set(PROJECT_PARTICIPANT.CREATED_AT, now)
                    .set(PROJECT_PARTICIPANT.UPDATED_AT, now)
                    .execute();
        });

        var mvcResult = mockMvc.perform(get("/api/v1/blog/reference"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String body = mvcResult.getResponse().getContentAsString();
        var dataArray = objectMapper.readTree(body).get("data");
        boolean found = false;
        if (dataArray != null && dataArray.isArray()) {
            for (var node : dataArray) {
                if (node.hasNonNull("referenceId")) {
                    long id = node.get("referenceId").asLong();
                    if (id == TEST_PROJECT_ID) {
                        found = true;
                        break;
                    }
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(found).isTrue();
    }
}


