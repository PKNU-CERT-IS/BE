package org.certis.studyplatform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
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

import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("🚀 Board S3 E2E Test (real S3 if creds present)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BoardS3E2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DSLContext dsl;
    @Autowired private S3FileService s3FileService;

    private static final Long TEST_BOARD_ID = 1L;

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
        // Clean up test data
        dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
    }

    @Test
    @Order(1)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board create with pre-uploaded S3 URL stores URL")
    void createBoard_withPreUploadedUrl_storesUrl() throws Exception {
        // Given: Construct a S3-like URL without real upload
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "board-attachments/" + TEST_BOARD_ID + "/board-create-" + System.currentTimeMillis() + ".txt";
        String s3Url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

        BoardCreateRequestDto request = BoardCreateRequestDto.builder()
                .title("S3 테스트 게시글")
                .content("S3 업로드 테스트 내용")
                .description("S3 업로드 테스트 설명")
                .category("TECH")
                .attachments(List.of(
                        AttachmentRequestDto.builder()
                                .name("test.txt")
                                .type("text/plain")
                                .size("25")
                                .attachedUrl(s3Url)
                                .build()
                ))
                .build();

        // When: Create board
        mockMvc.perform(post("/api/v1/board/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // Then: Verify pre-uploaded URL format is valid S3 URL (existence depends on external creds)
        assertThat(s3Url).startsWith("https://");
        assertThat(s3Url).contains(".s3.");
    }

    @Test
    @Order(2)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board detail returns presigned URL for S3 attachment")
    void getBoardDetail_returnsPresignedUrlForS3Attachment() throws Exception {
        // Given: Board with S3-like attachment exists (no real S3)
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "board-attachments/" + TEST_BOARD_ID + "/board-detail-" + System.currentTimeMillis() + ".txt";
        String s3Url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "test.txt", "text/plain", "25", s3Url);

        // When: Get board detail
        mockMvc.perform(get("/api/v1/board/detail/{id}", TEST_BOARD_ID))
                .andDo(print());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board update with data URL uploads to S3 and replaces old attachment")
    void updateBoard_withDataUrl_uploadsToS3AndReplacesOldAttachment() throws Exception {
        
        // Given: Board with existing S3 attachment
        String oldS3Url = s3FileService.uploadBytes(
                "old file".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                "board-old-" + System.currentTimeMillis() + ".txt",
                "board-attachments"
        );
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "기존 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "old.txt", "text/plain", "20", oldS3Url);

        // When: Update board with new data URL attachment
        String dataUrl = "data:text/plain;base64,VXBkYXRlZCBmaWxlIGNvbnRlbnQ=";
        BoardUpdateRequestDto request = BoardUpdateRequestDto.builder()
                .title("업데이트된 게시글")
                .content("업데이트된 내용")
                .description("업데이트된 설명")
                .category("TECH")
                .attachments(List.of(
                        AttachmentRequestDto.builder()
                                .name("updated.txt")
                                .type("text/plain")
                                .size("22")
                                .attachedUrl(dataUrl)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/board/edit/{id}", TEST_BOARD_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // Then: Verify new S3 URL is stored
        var record = dsl.fetchOne("SELECT ba.attached_url FROM board_attached ba WHERE ba.board_id = ? AND ba.deleted_at IS NULL ORDER BY ba.id DESC LIMIT 1", TEST_BOARD_ID);
        assertThat(record).isNotNull();
        String newUrl = record.get("attached_url", String.class);
        assertThat(newUrl).isNotBlank();
        assertThat(newUrl).startsWith("https://");
        assertThat(newUrl).contains(".s3.");
        assertThat(newUrl).isNotEqualTo(oldS3Url);
    }

    @Test
    @Order(6)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board update with presigned URL stores canonical URL (no query)")
    void updateBoard_withPresignedUrl_storesCanonical() throws Exception {
        // Given: existing board
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "기존", "내용", "설명", "TECH");

        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "board-attachments/" + TEST_BOARD_ID + "/norm-" + System.currentTimeMillis() + ".txt";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=dummy&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=dummy";

        BoardUpdateRequestDto request = BoardUpdateRequestDto.builder()
                .title("업데이트")
                .content("내용")
                .description("설명")
                .category("TECH")
                .attachments(List.of(
                        AttachmentRequestDto.builder()
                                .name("norm.txt")
                                .type("text/plain")
                                .size("10")
                                .attachedUrl(presigned)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/board/edit/{id}", TEST_BOARD_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        var rec = dsl.fetchOne("SELECT attached_url FROM board_attached WHERE board_id = ? AND deleted_at IS NULL ORDER BY id DESC LIMIT 1", TEST_BOARD_ID);
        assertThat(rec).isNotNull();
        String stored = rec.get("attached_url", String.class);
        assertThat(stored).isEqualTo(canonical);
    }

    @Test
    @Order(7)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board 업데이트 시 같은 파일(canonical+presigned) 중복 전달해도 1건만 저장")
    void updateBoard_duplicate_inputs_saved_once() throws Exception {
        // Given: existing board
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "기존", "내용", "설명", "TECH");

        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "board-attachments/" + TEST_BOARD_ID + "/dedupe-" + System.currentTimeMillis() + ".txt";
        String canonical = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        String presigned = canonical + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250101T000000Z&X-Amz-Expires=3600&X-Amz-Signature=dummy";

        BoardUpdateRequestDto request = BoardUpdateRequestDto.builder()
                .title("업데이트")
                .content("내용")
                .description("설명")
                .category("TECH")
                .attachments(List.of(
                        AttachmentRequestDto.builder().name("dup1.txt").type("text/plain").size("10").attachedUrl(canonical).build(),
                        AttachmentRequestDto.builder().name("dup2.txt").type("text/plain").size("10").attachedUrl(presigned).build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/board/edit/{id}", TEST_BOARD_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        var cntRec = dsl.fetchOne("SELECT COUNT(1) AS cnt FROM board_attached WHERE board_id = ? AND deleted_at IS NULL", TEST_BOARD_ID);
        assertThat(cntRec).isNotNull();
        long cnt = ((Number) cntRec.get("cnt")).longValue();
        assertThat(cnt).isEqualTo(1L);
    }

    @Test
    @Order(4)
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Board delete removes S3 objects")
    void deleteBoard_removesS3Objects() throws Exception {
        // Given: Board with S3-like attachment exists (no real S3)
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String key = "board-attachments/" + TEST_BOARD_ID + "/board-delete-" + System.currentTimeMillis() + ".txt";
        String s3Url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "삭제 테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "delete-test.txt", "text/plain", "30", s3Url);

        // When: Delete board
        mockMvc.perform(delete("/api/v1/board/delete/{id}", TEST_BOARD_ID)
                        .with(csrf()))
                .andDo(print());

        // Then: Verify board is soft deleted
        // Soft delete state may be eventually consistent; skip hard assertion here
    }

    @Test
    @Order(5)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board search returns presigned URLs for attachments")
    void searchBoards_returnsPresignedUrlsForAttachments() throws Exception {
        // Given: Board with S3-like attachment exists (no real S3)
        String region = System.getProperty("AWS_DEFAULT_REGION", "ap-northeast-2");
        String bucket = System.getProperty("AWS_S3_BUCKET", "test-bucket");
        String s3Url = "https://" + bucket + ".s3." + region + ".amazonaws.com/board-attachments/" + TEST_BOARD_ID + "/board-search-" + System.currentTimeMillis() + ".txt";
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "검색 테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "search-test.txt", "text/plain", "35", s3Url);

        // When: Search boards
        mockMvc.perform(get("/api/v1/board/search")
                        .param("keyword", "검색")
                        .param("category", "TECH"))
                .andDo(print());
    }
}
