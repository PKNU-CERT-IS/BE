package org.certis.studyplatform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectAttachedCreateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyUpdateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyAttachedCreateRequestDto;
import org.certis.studyplatform.shared.type.AttachedType;
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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 파일 업로드 → null 업데이트 → 삭제 확인 E2E 테스트
 *
 * 실제 S3를 이용한 파일 업로드/삭제 전체 과정을 테스트
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
@DisplayName("파일 업로드/삭제 E2E 테스트")
class FileUploadDeleteE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_PROJECT_ID = 2L;
    private static final Long TEST_STUDY_ID = 3L;

    // 환경변수 로드
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")  // 프로젝트 루트의 .env 파일
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
                .set(PROJECT.TITLE, "File Upload E2E Project")
                .set(PROJECT.DESCRIPTION, "Project for File Upload E2E")
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
                .set(STUDY.TITLE, "File Upload E2E Study")
                .set(STUDY.DESCRIPTION, "Study for File Upload E2E")
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
    @DisplayName("프로필 이미지 업로드 및 삭제 E2E 테스트")
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

        // Given: 실제 S3에 이미지 업로드
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String imageKey = String.format("profile/test-profile-%s.png", timestamp);
        String base64ImageData = createTestImageBase64();

        // S3에 실제 업로드
        String uploadedProfileImageUrl = uploadBase64ToS3(accessKeyId, secretAccessKey, region, bucket,
                base64ImageData, "image/png", imageKey);
        assertThat(uploadedProfileImageUrl).isNotNull();
        log.info("S3에 프로필 이미지 업로드 완료: {}", uploadedProfileImageUrl);

        // When: 프로필 업데이트 (업로드된 S3 URL 포함)
        ProfileUpdateRequestDto updateRequest = ProfileUpdateRequestDto.builder()
                .name("테스트사용자")
                .description("테스트용 사용자입니다")
                .profileImage(uploadedProfileImageUrl) // 실제 S3 URL 사용
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        log.info("프로필 이미지 업로드 및 업데이트 완료: {}", uploadedProfileImageUrl);

        // When: 프로필 이미지를 null로 설정하여 삭제
        ProfileUpdateRequestDto deleteRequest = ProfileUpdateRequestDto.builder()
                .name("테스트사용자")
                .description("테스트용 사용자입니다")
                .profileImage(null) // 핵심: null로 설정하여 삭제
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Then: S3에서 파일이 실제로 삭제되었는지 확인
        Thread.sleep(1000); // 삭제 처리 대기
        boolean fileExists = checkS3FileExists(accessKeyId, secretAccessKey, region, bucket, imageKey);
        assertThat(fileExists).isFalse();
        log.info("프로필 이미지 null로 업데이트 완료 - S3 파일 삭제 확인됨: {}", uploadedProfileImageUrl);
    }

    @Test
    @DisplayName("프로젝트 첨부파일 업로드 및 삭제 E2E 테스트")
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

        // Given: 실제 S3에 텍스트 파일 업로드
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileKey = String.format("project/test-project-%s.txt", timestamp);
        String base64TextData = createTestTextBase64();

        // S3에 실제 업로드
        String uploadedProjectAttachmentUrl = uploadBase64ToS3(accessKeyId, secretAccessKey, region, bucket,
                base64TextData, "text/plain", fileKey);
        assertThat(uploadedProjectAttachmentUrl).isNotNull();
        log.info("S3에 프로젝트 첨부파일 업로드 완료: {}", uploadedProjectAttachmentUrl);

        // When: 프로젝트 업데이트 (업로드된 S3 URL 포함)
        ProjectUpdateRequestDto updateRequest = new ProjectUpdateRequestDto();
        updateRequest.setProjectId(TEST_PROJECT_ID);
        updateRequest.setTitle("테스트 프로젝트");
        updateRequest.setDescription("테스트용 프로젝트입니다");

        // 첨부파일 DTO 생성 (S3 URL 포함)
        ProjectAttachedCreateRequestDto attachmentDto = new ProjectAttachedCreateRequestDto();
        attachmentDto.setName("test-project.txt");
        attachmentDto.setType(AttachedType.TEXT);
        attachmentDto.setSize(100L);
        attachmentDto.setAttachedUrl(uploadedProjectAttachmentUrl); // 실제 S3 URL 사용

        updateRequest.setAttachments(List.of(attachmentDto));

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        log.info("프로젝트 첨부파일 업로드 및 업데이트 완료: {}", uploadedProjectAttachmentUrl);

        // When: 프로젝트 첨부파일을 null로 설정 (이제는 보존 정책)
        ProjectUpdateRequestDto deleteRequest = new ProjectUpdateRequestDto();
        deleteRequest.setProjectId(TEST_PROJECT_ID);
        deleteRequest.setTitle("테스트 프로젝트");
        deleteRequest.setDescription("테스트용 프로젝트입니다");
        deleteRequest.setAttachments(null); // 핵심: null로 설정하여 삭제

        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Then: S3의 기존 파일은 유지되어야 함
        Thread.sleep(1000); // 처리 대기
        boolean fileExists = checkS3FileExists(accessKeyId, secretAccessKey, region, bucket, fileKey);
        assertThat(fileExists).isTrue();
        log.info("프로젝트 첨부파일 null로 업데이트 완료 - S3 파일 유지 확인됨: {}", uploadedProjectAttachmentUrl);
    }

    @Test
    @DisplayName("스터디 첨부파일 업로드 및 삭제 E2E 테스트")
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

        // Given: 실제 S3에 PDF 파일 업로드
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileKey = String.format("study/test-study-%s.pdf", timestamp);
        String base64PdfData = createTestPdfBase64();

        // S3에 실제 업로드
        String uploadedStudyAttachmentUrl = uploadBase64ToS3(accessKeyId, secretAccessKey, region, bucket,
                base64PdfData, "application/pdf", fileKey);
        assertThat(uploadedStudyAttachmentUrl).isNotNull();
        log.info("S3에 스터디 첨부파일 업로드 완료: {}", uploadedStudyAttachmentUrl);

        // When: 스터디 업데이트 (업로드된 S3 URL 포함)
        StudyUpdateRequestDto updateRequest = new StudyUpdateRequestDto();
        updateRequest.setStudyId(TEST_STUDY_ID);
        updateRequest.setTitle("테스트 스터디");
        updateRequest.setDescription("테스트용 스터디입니다");

        // 첨부파일 DTO 생성 (S3 URL 포함)
        StudyAttachedCreateRequestDto attachmentDto = new StudyAttachedCreateRequestDto();
        attachmentDto.setName("test-study.pdf");
        attachmentDto.setType(AttachedType.PDF);
        attachmentDto.setSize(200L);
        attachmentDto.setAttachedUrl(uploadedStudyAttachmentUrl); // 실제 S3 URL 사용

        updateRequest.setAttachments(List.of(attachmentDto));

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        log.info("스터디 첨부파일 업로드 및 업데이트 완료: {}", uploadedStudyAttachmentUrl);

        // When: 스터디 첨부파일을 null로 설정 (이제는 보존 정책)
        StudyUpdateRequestDto deleteRequest = new StudyUpdateRequestDto();
        deleteRequest.setStudyId(TEST_STUDY_ID);
        deleteRequest.setTitle("테스트 스터디");
        deleteRequest.setDescription("테스트용 스터디입니다");
        deleteRequest.setAttachments(null); // 핵심: null로 설정하여 삭제

        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Then: S3의 기존 파일은 유지되어야 함
        Thread.sleep(1000); // 처리 대기
        boolean fileExists = checkS3FileExists(accessKeyId, secretAccessKey, region, bucket, fileKey);
        assertThat(fileExists).isTrue();
        log.info("스터디 첨부파일 null로 업데이트 완료 - S3 파일 유지 확인됨: {}", uploadedStudyAttachmentUrl);
    }

    // ================================================================
    // S3 헬퍼 메서드들 (성공하는 테스트와 동일한 패턴)
    // ================================================================

    /**
     * Base64 데이터를 S3에 업로드하고 URL 반환
     */
    private String uploadBase64ToS3(String accessKeyId, String secretAccessKey, String region, String bucket,
                                    String base64Data, String contentType, String key) {
        try {
            // Base64 데이터에서 실제 바이트 추출
            String base64Content = base64Data;
            if (base64Data.startsWith("data:")) {
                base64Content = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            byte[] fileBytes = Base64.getDecoder().decode(base64Content);

            if (fileBytes.length == 0) {
                throw new IllegalArgumentException("파일이 비어있습니다.");
            }

            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);

            log.info("S3 업로드 완료: {}", url);
            return url;

        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    /**
     * S3 파일 존재 여부 확인
     */
    private boolean checkS3FileExists(String accessKeyId, String secretAccessKey, String region, String bucket, String key) {
        try {
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("S3 파일 존재 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 테스트용 이미지 데이터 생성 (Base64)
     */
    private String createTestImageBase64() {
        // 간단한 1x1 픽셀 PNG 이미지 (Base64)
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }

    /**
     * 테스트용 텍스트 파일 데이터 생성 (Base64)
     */
    private String createTestTextBase64() {
        String content = "이것은 테스트용 텍스트 파일입니다.\n" +
                "업로드 시간: " + LocalDateTime.now() + "\n" +
                "테스트 ID: " + System.currentTimeMillis();
        return "data:text/plain;base64," + Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 테스트용 PDF 파일 데이터 생성 (Base64)
     */
    private String createTestPdfBase64() {
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
        return "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfContent.getBytes(StandardCharsets.UTF_8));
    }
}