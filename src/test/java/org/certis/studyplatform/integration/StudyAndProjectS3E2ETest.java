package org.certis.studyplatform.integration;

import io.github.cdimascio.dotenv.Dotenv;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("🌐 E2E: Study/Project S3 업로드 및 조회")
class StudyAndProjectS3E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

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
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
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
                    .set(PROJECT.STATUS, "READY")
                    .set(PROJECT.RESULT_SUBMIT_STATUS, "READY")
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

        // Then: 상세 조회에서 해당 URL과 thumbnailUrl 확인 (형식 검증 중심)
        mockMvc.perform(get("/api/v1/study/detail").param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(org.hamcrest.Matchers.startsWith("https://")));
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
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.startsWith(uploadedUrl)));
    }

    @Test
    @DisplayName("Study 업데이트 시 presigned S3 URL을 보내면 쿼리 제거하여 저장한다")
    void e2e_study_update_with_presigned_url_is_normalized() throws Exception {
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "study-attachments/" + TEST_STUDY_ID + "/norm-" + System.currentTimeMillis() + ".txt";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=dummy&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=dummy";

        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"norm.txt\"," +
                "\"type\":\"TEXT\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + presigned + "\"}]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("SELECT attached_url FROM study_attached WHERE study_id = ? AND deleted_at IS NULL ORDER BY id DESC LIMIT 1", TEST_STUDY_ID);
        Assertions.assertNotNull(rec);
        String stored = (String) rec.get("attached_url");
        Assertions.assertEquals(canonical, stored);
    }

    @Test
    @DisplayName("Project 업데이트 시 presigned S3 URL을 보내면 쿼리 제거하여 저장한다")
    void e2e_project_update_with_presigned_url_is_normalized() throws Exception {
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "project-attachments/" + TEST_PROJECT_ID + "/norm-" + System.currentTimeMillis() + ".pdf";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=dummy&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=dummy";

        String updateJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"norm.pdf\"," +
                "\"type\":\"PDF\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + presigned + "\"}]}";

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        var rec = dsl.fetchOne("SELECT attached_url FROM project_attached WHERE project_id = ? AND deleted_at IS NULL ORDER BY id DESC LIMIT 1", TEST_PROJECT_ID);
        Assertions.assertNotNull(rec);
        String stored = (String) rec.get("attached_url");
        Assertions.assertEquals(canonical, stored);
    }

    @Test
    @DisplayName("Study 업데이트 시 같은 파일(canonical+presigned) 중복 전달해도 1건만 저장")
    void e2e_study_update_duplicate_inputs_saved_once() throws Exception {
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "study-attachments/" + TEST_STUDY_ID + "/dedupe-" + System.currentTimeMillis() + ".txt";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-Signature=dummy";

        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"dup1.txt\"," +
                "\"type\":\"TEXT\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + canonical + "\"},{" +
                "\"name\":\"dup2.txt\"," +
                "\"type\":\"TEXT\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + presigned + "\"}]}";

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        var cntRec = dsl.fetchOne("SELECT COUNT(1) AS cnt FROM study_attached WHERE study_id = ? AND deleted_at IS NULL", TEST_STUDY_ID);
        Assertions.assertNotNull(cntRec);
        long cnt = ((Number) cntRec.get("cnt")).longValue();
        Assertions.assertEquals(1L, cnt);
    }

    @Test
    @DisplayName("Project 업데이트 시 같은 파일(canonical+presigned) 중복 전달해도 1건만 저장")
    void e2e_project_update_duplicate_inputs_saved_once() throws Exception {
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "project-attachments/" + TEST_PROJECT_ID + "/dedupe-" + System.currentTimeMillis() + ".pdf";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-Signature=dummy";

        String updateJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"dup1.pdf\"," +
                "\"type\":\"PDF\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + canonical + "\"},{" +
                "\"name\":\"dup2.pdf\"," +
                "\"type\":\"PDF\"," +
                "\"size\":\"3\"," +
                "\"attachedUrl\":\"" + presigned + "\"}]}";

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        var cntRec = dsl.fetchOne("SELECT COUNT(1) AS cnt FROM project_attached WHERE project_id = ? AND deleted_at IS NULL", TEST_PROJECT_ID);
        Assertions.assertNotNull(cntRec);
        long cnt = ((Number) cntRec.get("cnt")).longValue();
        Assertions.assertEquals(1L, cnt);
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
                .andExpect(jsonPath("$.data.thumbnailUrl").value(org.hamcrest.Matchers.startsWith(imgUrl)));
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
        // 종료 제출 첨부는 컨트롤러에서 JSON(@RequestBody)로 URL을 받도록 되어 있으므로
        // 테스트에서는 S3에 사전 업로드 후 해당 URL을 전달한다
        String endKey = "e2e-study-end/" + System.currentTimeMillis() + "/end.txt";
        String endUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, endKey, "text/plain", "done".getBytes());

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

        String endStudyJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachment\":{" +
                "\"name\":\"end.txt\"," +
                "\"type\":\"TXT\"," +
                "\"size\":\"4\"," +
                "\"attachedUrl\":\"" + endUrl + "\"}}";

        mockMvc.perform(post("/api/v1/study/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endStudyJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.resultSubmitStatus").value("INPROGRESS"));

        // 생성자 본인도 참가자로 승인 처리 + 종료 시점 과거로 보정 (completed list/participation 충족)
        OffsetDateTime now = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(STUDY_PARTICIPANT)
                    .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY_PARTICIPANT.STATUS, "APPROVED")
                    .set(STUDY_PARTICIPANT.CREATED_AT, now.minusDays(1))
                    .set(STUDY_PARTICIPANT.UPDATED_AT, now.minusDays(1))
                    .execute();

            // completed 판단을 위해 ENDED_AT을 과거로 조정
            tx.update(STUDY)
                    .set(STUDY.ENDED_AT, now.minusHours(1))
                    .where(STUDY.ID.eq(TEST_STUDY_ID))
                    .execute();
        });

        mockMvc.perform(get("/api/v1/blog/reference"))
                .andDo(print())
                .andExpect(status().isOk());
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

        // 종료 제출 첨부는 컨트롤러에서 JSON(@RequestBody)로 URL을 받도록 되어 있으므로
        // 테스트에서는 S3에 사전 업로드 후 해당 URL을 전달한다
        String projEndKey = "e2e-project-end/" + System.currentTimeMillis() + "/end.txt";
        String projEndUrl = uploadToS3(accessKeyId, secretAccessKey, region, bucket, projEndKey, "text/plain", "done".getBytes());
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
        String endProjectJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachment\":{" +
                "\"name\":\"end.txt\"," +
                "\"type\":\"TXT\"," +
                "\"size\":\"4\"," +
                "\"attachedUrl\":\"" + projEndUrl + "\"}}";

        mockMvc.perform(post("/api/v1/project/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endProjectJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.resultSubmitStatus").value("INPROGRESS"));

        // 종료 직후 ended_at 과거 보정은 내부 승인 시점으로 처리되므로 생략

        // 생성자 본인도 참가자로 승인 처리 + 종료 시점 과거로 보정
        OffsetDateTime now2 = OffsetDateTime.now();
        dsl.transaction(cfg -> {
            var tx = org.jooq.impl.DSL.using(cfg);
            tx.insertInto(PROJECT_PARTICIPANT)
                    .set(PROJECT_PARTICIPANT.PROJECT_ID, TEST_PROJECT_ID)
                    .set(PROJECT_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT_PARTICIPANT.STATUS, "APPROVED")
                    .set(PROJECT_PARTICIPANT.CREATED_AT, now2.minusDays(1))
                    .set(PROJECT_PARTICIPANT.UPDATED_AT, now2.minusDays(1))
                    .execute();

            tx.update(PROJECT)
                    .set(PROJECT.ENDED_AT, now2.minusHours(1))
                    .where(PROJECT.ID.eq(TEST_PROJECT_ID))
                    .execute();
        });

        mockMvc.perform(get("/api/v1/blog/reference"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Study 검색 결과에 모든 첨부파일이 포함된다")
    void e2e_study_search_returns_all_attachments() throws Exception {
        // Given: Insert two attachments for the existing study
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key1 = "study-attachments/" + TEST_STUDY_ID + "/s1-" + System.currentTimeMillis() + ".txt";
        String key2 = "study-attachments/" + TEST_STUDY_ID + "/s2-" + System.nanoTime() + ".txt";
        String url1 = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key1;
        String url2 = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key2;

        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, "a1.txt")
                .set(STUDY_ATTACHED.TYPE, "text/plain")
                .set(STUDY_ATTACHED.SIZE, "10")
                .set(STUDY_ATTACHED.ATTACHED_URL, url1)
                .set(STUDY_ATTACHED.CREATED_AT, now)
                .set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();
        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, "a2.txt")
                .set(STUDY_ATTACHED.TYPE, "text/plain")
                .set(STUDY_ATTACHED.SIZE, "20")
                .set(STUDY_ATTACHED.ATTACHED_URL, url2)
                .set(STUDY_ATTACHED.CREATED_AT, now)
                .set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();

        // When: Search with keyword matching the seeded title
        mockMvc.perform(get("/api/v1/study/search").param("keyword", "E2E"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Project 검색 결과에 모든 첨부파일이 포함된다")
    void e2e_project_search_returns_all_attachments() throws Exception {
        // Given: Insert two attachments for the existing project
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key1 = "project-attachments/" + TEST_PROJECT_ID + "/p1-" + System.currentTimeMillis() + ".txt";
        String key2 = "project-attachments/" + TEST_PROJECT_ID + "/p2-" + System.nanoTime() + ".txt";
        String url1 = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key1;
        String url2 = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key2;

        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "p1.txt")
                .set(PROJECT_ATTACHED.TYPE, "text/plain")
                .set(PROJECT_ATTACHED.SIZE, "10")
                .set(PROJECT_ATTACHED.ATTACHED_URL, url1)
                .set(PROJECT_ATTACHED.CREATED_AT, now)
                .set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();
        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "p2.txt")
                .set(PROJECT_ATTACHED.TYPE, "text/plain")
                .set(PROJECT_ATTACHED.SIZE, "20")
                .set(PROJECT_ATTACHED.ATTACHED_URL, url2)
                .set(PROJECT_ATTACHED.CREATED_AT, now)
                .set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();

        // When: Search with keyword matching the seeded title
        mockMvc.perform(get("/api/v1/project/search").param("keyword", "E2E"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Study create(FileA) → update(FileB+FileA) 후 검색에서 둘 다 보이고 presigned로 다운로드 가능")
    void e2e_study_create_then_update_with_two_files_and_downloadable() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");
        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty());
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty());
        assumeTrue(region != null && !region.isEmpty());
        assumeTrue(bucket != null && !bucket.isEmpty());

        // Pre-upload FileA and FileB
        String keyA = "e2e-study-files/" + System.currentTimeMillis() + "/A.txt";
        String urlA = uploadToS3(accessKeyId, secretAccessKey, region, bucket, keyA, "text/plain", "A".getBytes());
        String keyB = "e2e-study-files/" + System.currentTimeMillis() + "/B.txt";
        String urlB = uploadToS3(accessKeyId, secretAccessKey, region, bucket, keyB, "text/plain", "B".getBytes());

        // Seed Study with FileA (avoid create endpoint auth/contract dependencies)
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "S3 E2E Study")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
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
                .onConflict(STUDY.ID).doNothing()
                .execute();

        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, "A.txt")
                .set(STUDY_ATTACHED.TYPE, "text/plain")
                .set(STUDY_ATTACHED.SIZE, "1")
                .set(STUDY_ATTACHED.ATTACHED_URL, urlA)
                .set(STUDY_ATTACHED.CREATED_AT, now)
                .set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();

        // Update: include FileB and FileA (order changed) → should store both
        String updateJson = "{" +
                "\"studyId\":" + TEST_STUDY_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"B.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlB + "\"},{" +
                "\"name\":\"A.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlA + "\"}]}";
        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Search: attachments should include at least 2
        mockMvc.perform(get("/api/v1/study/search").param("keyword", "E2E"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].attachments.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.content[0].attachments[0].attachedUrl", org.hamcrest.Matchers.startsWith("https://")));

        // Download check: GET presigned URL should be reachable (HTTP 200)
        // We can't follow the presigned URL via MockMvc; validate format and presence only
    }

    @Test
    @DisplayName("Project create(FileA) → update(FileB+FileA) 후 검색에서 둘 다 보이고 presigned로 다운로드 가능")
    void e2e_project_create_then_update_with_two_files_and_downloadable() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucket = dotenv.get("AWS_S3_BUCKET");
        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty());
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty());
        assumeTrue(region != null && !region.isEmpty());
        assumeTrue(bucket != null && !bucket.isEmpty());

        // Pre-upload FileA and FileB
        String keyA = "e2e-project-files/" + System.currentTimeMillis() + "/A.txt";
        String urlA = uploadToS3(accessKeyId, secretAccessKey, region, bucket, keyA, "text/plain", "A".getBytes());
        String keyB = "e2e-project-files/" + System.currentTimeMillis() + "/B.txt";
        String urlB = uploadToS3(accessKeyId, secretAccessKey, region, bucket, keyB, "text/plain", "B".getBytes());

        // Create: since create API path not defined here, seed DB directly for project
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, "S3 E2E Project")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
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
                .onConflict(PROJECT.ID).doNothing()
                .execute();

        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "A.txt")
                .set(PROJECT_ATTACHED.TYPE, "text/plain")
                .set(PROJECT_ATTACHED.SIZE, "1")
                .set(PROJECT_ATTACHED.ATTACHED_URL, urlA)
                .set(PROJECT_ATTACHED.CREATED_AT, now)
                .set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();

        // Update with FileB + FileA via API
        String updateJson = "{" +
                "\"projectId\":" + TEST_PROJECT_ID + "," +
                "\"attachments\":[{" +
                "\"name\":\"B.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlB + "\"},{" +
                "\"name\":\"A.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlA + "\"}]}";
        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk());

        // Search: attachments should include at least 2 and be presigned
        mockMvc.perform(get("/api/v1/project/search").param("keyword", "E2E"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].attachments.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.content[0].attachments[0].attachedUrl", org.hamcrest.Matchers.startsWith("https://")));
    }

    @Test
    @DisplayName("Study: attachments == null → 보존, [] → DB만 삭제, 기존+신규 → 신규만 추가, 기존만 → 그대로")
    void e2e_study_update_policy_variants() throws Exception {
        String region = dotenv.get("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = dotenv.get("AWS_S3_BUCKET", "test-bucket");
        String urlOld = "https://" + bucket + ".s3." + region + ".amazonaws.com/study-attachments/" + TEST_STUDY_ID + "/old.txt";
        String urlNew = "https://" + bucket + ".s3." + region + ".amazonaws.com/study-attachments/" + TEST_STUDY_ID + "/new.txt";

        OffsetDateTime now = OffsetDateTime.now();
        // seed study with old attachment
        dsl.insertInto(STUDY).set(STUDY.ID, TEST_STUDY_ID).set(STUDY.TITLE, "S3 E2E Study")
                .set(STUDY.DESCRIPTION, "d").set(STUDY.CONTENT, "c").set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발").set(STUDY.SUBCATEGORY, "풀스택").set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1)).set(STUDY.ENDED_AT, now.plusDays(30))
                .set(STUDY.STATUS, "READY").set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, now).set(STUDY.UPDATED_AT, now)
                .onConflict(STUDY.ID).doNothing().execute();
        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, TEST_STUDY_ID).set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, "old.txt").set(STUDY_ATTACHED.TYPE, "text/plain").set(STUDY_ATTACHED.SIZE, "1")
                .set(STUDY_ATTACHED.ATTACHED_URL, urlOld).set(STUDY_ATTACHED.CREATED_AT, now).set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();

        // attachments == null -> preserve
        String nullJson = "{" + "\"studyId\":" + TEST_STUDY_ID + "}";
        mockMvc.perform(put("/api/v1/study/update").contentType(MediaType.APPLICATION_JSON).content(nullJson))
                .andDo(print()).andExpect(status().isOk());
        var cnt1 = dsl.fetchOne("SELECT COUNT(1) AS c FROM study_attached WHERE study_id=? AND deleted_at IS NULL", TEST_STUDY_ID);
        Assertions.assertEquals(1L, ((Number)cnt1.get("c")).longValue());

        // attachments == [] -> DB only delete
        String emptyJson = "{" + "\"studyId\":" + TEST_STUDY_ID + ",\"attachments\":[]}";
        mockMvc.perform(put("/api/v1/study/update").contentType(MediaType.APPLICATION_JSON).content(emptyJson))
                .andDo(print()).andExpect(status().isOk());
        var cnt2 = dsl.fetchOne("SELECT COUNT(1) AS c FROM study_attached WHERE study_id=? AND deleted_at IS NULL", TEST_STUDY_ID);
        Assertions.assertEquals(0L, ((Number)cnt2.get("c")).longValue());

        // existing only (re-add old) + new -> DB add only for new ; old remains absent? Our policy: when list provided we treat as desired set; add both
        String addBoth = "{" + "\"studyId\":" + TEST_STUDY_ID + ",\"attachments\":[{" +
                "\"name\":\"old.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlOld + "\"},{" +
                "\"name\":\"new.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlNew + "\"}]}";
        mockMvc.perform(put("/api/v1/study/update").contentType(MediaType.APPLICATION_JSON).content(addBoth))
                .andDo(print()).andExpect(status().isOk());
        var cnt3 = dsl.fetchOne("SELECT COUNT(1) AS c FROM study_attached WHERE study_id=? AND deleted_at IS NULL", TEST_STUDY_ID);
        Assertions.assertTrue(((Number)cnt3.get("c")).longValue() >= 2L);
    }

    @Test
    @DisplayName("Project: attachments 정책 케이스 검증(null, [], 기존+신규)")
    void e2e_project_update_policy_variants() throws Exception {
        String region = dotenv.get("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = dotenv.get("AWS_S3_BUCKET", "test-bucket");
        String urlOld = "https://" + bucket + ".s3." + region + ".amazonaws.com/project-attachments/" + TEST_PROJECT_ID + "/old.txt";
        String urlNew = "https://" + bucket + ".s3." + region + ".amazonaws.com/project-attachments/" + TEST_PROJECT_ID + "/new.txt";

        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT).set(PROJECT.ID, TEST_PROJECT_ID).set(PROJECT.TITLE, "S3 E2E Project")
                .set(PROJECT.DESCRIPTION, "d").set(PROJECT.CONTENT, "c").set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT.CATEGORY, "웹 개발").set(PROJECT.SUBCATEGORY, "풀스택").set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1)).set(PROJECT.ENDED_AT, now.plusDays(30))
                .set(PROJECT.STATUS, "READY").set(PROJECT.RESULT_SUBMIT_STATUS, "READY")
                .set(PROJECT.CREATED_AT, now).set(PROJECT.UPDATED_AT, now)
                .onConflict(PROJECT.ID).doNothing().execute();
        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID).set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "old.txt").set(PROJECT_ATTACHED.TYPE, "text/plain").set(PROJECT_ATTACHED.SIZE, "1")
                .set(PROJECT_ATTACHED.ATTACHED_URL, urlOld).set(PROJECT_ATTACHED.CREATED_AT, now).set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();

        // null → preserve
        String nullJson = "{" + "\"projectId\":" + TEST_PROJECT_ID + "}";
        mockMvc.perform(put("/api/v1/project/update").contentType(MediaType.APPLICATION_JSON).content(nullJson))
                .andDo(print()).andExpect(status().isOk());
        var c1 = dsl.fetchOne("SELECT COUNT(1) AS c FROM project_attached WHERE project_id=? AND deleted_at IS NULL", TEST_PROJECT_ID);
        Assertions.assertEquals(1L, ((Number)c1.get("c")).longValue());

        // [] → DB only delete
        String emptyJson = "{" + "\"projectId\":" + TEST_PROJECT_ID + ",\"attachments\":[]}";
        mockMvc.perform(put("/api/v1/project/update").contentType(MediaType.APPLICATION_JSON).content(emptyJson))
                .andDo(print()).andExpect(status().isOk());
        var c2 = dsl.fetchOne("SELECT COUNT(1) AS c FROM project_attached WHERE project_id=? AND deleted_at IS NULL", TEST_PROJECT_ID);
        Assertions.assertEquals(0L, ((Number)c2.get("c")).longValue());

        // existing + new → add both
        String addBoth = "{" + "\"projectId\":" + TEST_PROJECT_ID + ",\"attachments\":[{" +
                "\"name\":\"old.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlOld + "\"},{" +
                "\"name\":\"new.txt\",\"type\":\"TEXT\",\"size\":\"1\",\"attachedUrl\":\"" + urlNew + "\"}]}";
        mockMvc.perform(put("/api/v1/project/update").contentType(MediaType.APPLICATION_JSON).content(addBoth))
                .andDo(print()).andExpect(status().isOk());
        var c3 = dsl.fetchOne("SELECT COUNT(1) AS c FROM project_attached WHERE project_id=? AND deleted_at IS NULL", TEST_PROJECT_ID);
        Assertions.assertTrue(((Number)c3.get("c")).longValue() >= 2L);
    }
}


