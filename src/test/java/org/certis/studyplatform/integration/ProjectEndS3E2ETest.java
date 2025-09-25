package org.certis.studyplatform.integration;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🚀 Project End S3 E2E Test (real S3 if creds present)")
class ProjectEndS3E2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private S3FileService s3FileService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_BASE = "/api/v1/project";
    private static final Long STAFF_ID = 3L; // WithMockUser("staff") maps to 3L in tests
    private static final Long PROJECT_ID = 1L;

    // 환경변수 로드 - S3FileUploadDeleteE2ETest와 동일한 패턴
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
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
        // 테스트 데이터 초기화
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        // admin/staff member as project creator
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

        // project owned by staff
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, PROJECT_ID)
                .set(PROJECT.TITLE, "P1")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, STAFF_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/project/end with data URL uploads to real S3 and stores URL when creds exist")
    void end_uploads_to_s3_and_stores_urls() throws Exception {
        // S3FileUploadIntegrationTest와 동일한 방식으로 자격증명 확인
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucketName = dotenv.get("AWS_S3_BUCKET");
        
        // 환경변수 확인 - S3FileUploadIntegrationTest와 동일한 패턴
        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(),
                "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(),
                "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(),
                "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucketName != null && !bucketName.isEmpty(),
                "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // S3 버킷 존재 여부는 실제 API 호출로만 확인 (S3FileService.bucketExists() 사용 안함)
        // Assumptions.assumeTrue(s3FileService.bucketExists(), 
        //         "Skipping: S3 bucket does not exist or is not accessible");

        String dataUrl = "data:text/plain;base64,SGVsbG8gd29ybGQ="; // Hello world
        String body = "{\"projectId\":" + PROJECT_ID + ",\"attachment\":{\"attachedUrl\":\"" + dataUrl + "\",\"name\":\"report.pdf\",\"type\":\"application/pdf\",\"size\":\"1234\"}}";

        mockMvc.perform(post(USER_BASE + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        // DB에서 결과 확인
        var rec = dsl.fetchOne("select result_submit_status, result_attached_url from project where id=?", PROJECT_ID);
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("INPROGRESS");
        
        String url = rec.get("result_attached_url", String.class);
        assertThat(url).isNotBlank();
        assertThat(url).startsWith("https://");
        assertThat(url).contains(bucketName);
        
        // S3에 실제로 파일이 업로드되었는지 확인
        assertThat(s3FileService.fileExists(url)).isTrue();
        
        log.info("✅ S3 파일 업로드 및 URL 저장 테스트 성공: {}", url);
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/project/end (data URL) then GET detail returns status and S3 URL")
    void end_then_detail_returns_status_and_url() throws Exception {
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucketName = dotenv.get("AWS_S3_BUCKET");

        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(), "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(), "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(), "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucketName != null && !bucketName.isEmpty(), "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        String dataUrl = "data:text/plain;base64,ZG9uZQ=="; // done
        String body = "{\"projectId\":" + PROJECT_ID + ",\"attachment\":{\"attachedUrl\":\"" + dataUrl + "\",\"name\":\"report.pdf\",\"type\":\"application/pdf\",\"size\":\"1234\"}}";
        mockMvc.perform(post(USER_BASE + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(USER_BASE + "/detail").param("projectId", String.valueOf(PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultSubmitStatus").value("INPROGRESS"));

        // DB에서 URL 재확인 (detail 응답에는 별도 필드로 노출되지 않음)
        var rec = dsl.fetchOne("select result_attached_url from project where id=?", PROJECT_ID);
        String url = rec.get("result_attached_url", String.class);
        assertThat(url).isNotBlank();
        assertThat(url).startsWith("https://");
        assertThat(url).contains(bucketName);
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/project/end without attachment returns 400 Bad Request")
    void end_without_file_returns_bad_request() throws Exception {
        String body = "{\"projectId\":" + PROJECT_ID + "}";
        mockMvc.perform(post(USER_BASE + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/project/end handles data URL types (stores URL)")
    void end_handles_multiple_file_types() throws Exception {
        // S3FileUploadIntegrationTest와 동일한 방식으로 자격증명 확인
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucketName = dotenv.get("AWS_S3_BUCKET");
        
        // 환경변수 확인
        assumeTrue(accessKeyId != null && !accessKeyId.isEmpty(),
                "AWS_ACCESS_KEY_ID 환경변수가 설정되지 않았습니다.");
        assumeTrue(secretAccessKey != null && !secretAccessKey.isEmpty(),
                "AWS_SECRET_ACCESS_KEY 환경변수가 설정되지 않았습니다.");
        assumeTrue(region != null && !region.isEmpty(),
                "AWS_DEFAULT_REGION 환경변수가 설정되지 않았습니다.");
        assumeTrue(bucketName != null && !bucketName.isEmpty(),
                "AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");

        // S3 버킷 존재 여부 확인
        Assumptions.assumeTrue(s3FileService.bucketExists(), 
                "Skipping: S3 bucket does not exist or is not accessible");

        String pdfBase64 = java.util.Base64.getEncoder().encodeToString(createMockPdfData());
        String dataUrl = "data:application/pdf;base64," + pdfBase64;
        String body = "{\"projectId\":" + PROJECT_ID + ",\"attachment\":{\"attachedUrl\":\"" + dataUrl + "\",\"name\":\"report.pdf\",\"type\":\"application/pdf\",\"size\":\"1234\"}}";

        mockMvc.perform(post(USER_BASE + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk());

        // DB에서 결과 확인 (단일 URL 저장 확인)
        var rec = dsl.fetchOne("select result_submit_status, result_attached_url from project where id=?", PROJECT_ID);
        assertThat(rec.get("result_submit_status", String.class)).isEqualTo("INPROGRESS");
        
        String url = rec.get("result_attached_url", String.class);
        assertThat(url).isNotBlank();
        assertThat(url).startsWith("https://");
        assertThat(s3FileService.fileExists(url)).isTrue();

        log.info("✅ 다중 파일 타입 업로드 테스트 성공 (첫 URL 저장 확인)");
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("/project/end works without AWS credentials (fallback behavior)")
    void end_works_without_aws_credentials() throws Exception {
        // AWS 자격증명 체크 - 없으면 이 테스트 실행
        String accessKeyId = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretAccessKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_DEFAULT_REGION");
        String bucketName = dotenv.get("AWS_S3_BUCKET");

        boolean hasCredentials = accessKeyId != null && !accessKeyId.isEmpty() &&
                secretAccessKey != null && !secretAccessKey.isEmpty() &&
                region != null && !region.isEmpty() &&
                bucketName != null && !bucketName.isEmpty();

        if (hasCredentials) {
            // 크레덴셜이 있는 경우에도 테스트를 수행하여 정상 동작 확인
            String dataUrl = "data:text/plain;base64,SGVsbG8gd29ybGQ=";
            String body = "{\"projectId\":" + PROJECT_ID + ",\"attachment\":{\"attachedUrl\":\"" + dataUrl + "\",\"name\":\"report.pdf\",\"type\":\"application/pdf\",\"size\":\"1234\"}}";
            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk());
            var rec = dsl.fetchOne("select result_attached_url from project where id=?", PROJECT_ID);
            String url = rec.get("result_attached_url", String.class);
            assertThat(url).isNotBlank();
            assertThat(url).startsWith("https://");
            assertThat(s3FileService.fileExists(url)).isTrue();
            log.info("✅ AWS 자격증명 있는 환경에서의 동작 테스트 완료");
        } else {
            // 크레덴셜이 없는 경우 기대 동작(에러 또는 graceful 실패) 확인
            String dataUrl = "data:text/plain;base64,SGVsbG8gd29ybGQ=";
            String body = "{\"projectId\":" + PROJECT_ID + ",\"attachment\":{\"attachedUrl\":\"" + dataUrl + "\",\"name\":\"report.pdf\",\"type\":\"application/pdf\",\"size\":\"1234\"}}";
            mockMvc.perform(post(USER_BASE + "/end")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
            log.info("✅ AWS 자격증명 없는 환경에서의 fallback 동작 테스트 완료");
        }
    }

    // ================================================================
    // 헬퍼 메서드들 - S3FileUploadIntegrationTest와 동일한 패턴
    // ================================================================

    /**
     * S3FileUploadIntegrationTest와 동일한 패턴으로 환경변수 체크
     */
    private void assumeTrue(boolean condition, String message) {
        if (!condition) {
            log.warn("테스트 건너뜀: {}", message);
            Assumptions.assumeTrue(false, message);
        }
    }

    // setEnv 리플렉션 헬퍼는 불필요하여 제거 (시스템 프로퍼티 사용)

    private byte[] createMockPdfData() {
        String pdfContent = """
        %PDF-1.4
        1 0 obj
        <<
        /Type /Catalog
        /Pages 2 0 R
        >>
        endobj

        2 0 obj
        <<
        /Type /Pages
        /Kids [3 0 R]
        /Count 1
        >>
        endobj

        3 0 obj
        <<
        /Type /Page
        /Parent 2 0 R
        /MediaBox [0 0 612 792]
        >>
        endobj

        xref
        0 4
        0000000000 65535 f
        0000000010 00000 n
        0000000079 00000 n
        0000000173 00000 n
        trailer
        <<
        /Size 4
        /Root 1 0 R
        >>
        startxref
        253
        %%EOF
        """;
        return pdfContent.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createMockZipData() {
        // 간단한 ZIP 파일 구조 생성
        byte[] zipHeader = {0x50, 0x4B, 0x03, 0x04}; // PK\003\004
        byte[] mockZipData = new byte[256];

        System.arraycopy(zipHeader, 0, mockZipData, 0, zipHeader.length);
        for (int i = zipHeader.length; i < mockZipData.length - 4; i++) {
            mockZipData[i] = (byte) (i % 256);
        }

        // ZIP 중앙 디렉토리 종료 시그니처: PK\005\006
        mockZipData[mockZipData.length - 4] = 0x50;
        mockZipData[mockZipData.length - 3] = 0x4B;
        mockZipData[mockZipData.length - 2] = 0x05;
        mockZipData[mockZipData.length - 1] = 0x06;

        return mockZipData;
    }

    private byte[] createMockImageData() {
        // 가짜 JPEG 헤더와 데이터 생성
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] mockData = new byte[1024];
        System.arraycopy(jpegHeader, 0, mockData, 0, jpegHeader.length);

        for (int i = jpegHeader.length; i < mockData.length - 2; i++) {
            mockData[i] = (byte) (i % 256);
        }

        // JPEG 종료 마커
        mockData[mockData.length - 2] = (byte) 0xFF;
        mockData[mockData.length - 1] = (byte) 0xD9;

        return mockData;
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    @DisplayName("Admin /end endpoint returns only INPROGRESS projects, filters out other statuses")
    void admin_end_endpoint_filters_inprogress_projects() throws Exception {
        // Create multiple projects with different statuses
        Long projectId2 = 2L;
        Long projectId3 = 3L;
        Long projectId4 = 4L;

        // Project 1: INPROGRESS (should be returned)
        dsl.update(PROJECT)
                .set(PROJECT.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(PROJECT.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusHours(1))
                .set(PROJECT.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/project1.txt")
                .where(PROJECT.ID.eq(PROJECT_ID))
                .execute();

        // Project 2: INPROGRESS (should be returned)
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId2)
                .set(PROJECT.TITLE, "P2")
                .set(PROJECT.DESCRIPTION, "desc2")
                .set(PROJECT.CONTENT, "content2")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, STAFF_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(PROJECT.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusMinutes(30))
                .set(PROJECT.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/project2.txt")
                .execute();

        // Project 3: COMPLETED (should NOT be returned)
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId3)
                .set(PROJECT.TITLE, "P3")
                .set(PROJECT.DESCRIPTION, "desc3")
                .set(PROJECT.CONTENT, "content3")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, STAFF_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.RESULT_SUBMIT_STATUS, "COMPLETED")
                .set(PROJECT.RESULT_SUBMITTED_AT, java.time.OffsetDateTime.now().minusDays(1))
                .set(PROJECT.RESULT_ATTACHED_URL, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/project3.txt")
                .execute();

        // Project 4: NULL status (should NOT be returned)
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId4)
                .set(PROJECT.TITLE, "P4")
                .set(PROJECT.DESCRIPTION, "desc4")
                .set(PROJECT.CONTENT, "content4")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, java.time.OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, STAFF_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, java.time.OffsetDateTime.now())
                .setNull(PROJECT.RESULT_SUBMIT_STATUS)
                .setNull(PROJECT.RESULT_SUBMITTED_AT)
                .setNull(PROJECT.RESULT_ATTACHED_URL)
                .execute();

        // Test the admin endpoint
        var result = mockMvc.perform(get("/api/v1/admin/project/end"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response to verify only INPROGRESS projects are returned
        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        var data = response.get("data");
        
        // Should return exactly 2 projects (projectId 1 and 2)
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);
        
        // Verify the returned projects have INPROGRESS status
        var projectIds = new java.util.HashSet<Long>();
        for (var project : data) {
            Long id = project.get("projectId").asLong();
            String status = project.get("status").asText();
            projectIds.add(id);
            assertThat(status).isEqualTo("INPROGRESS");
        }
        
        // Verify we got the correct projects
        assertThat(projectIds).containsExactlyInAnyOrder(PROJECT_ID, projectId2);
        assertThat(projectIds).doesNotContain(projectId3, projectId4);
    }
}