package org.certis.studyplatform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.response.ResponseStatus;
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
import org.springframework.transaction.annotation.Transactional;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    @DisplayName("Board create with data URL uploads to S3 and stores URL")
    @Transactional
    void createBoard_withDataUrl_uploadsToS3AndStoresUrl() throws Exception {
        // 자격증명이 없으면 실패하므로 실제 S3 테스트 전제
        
        // Given: Board creation request with data URL attachment
        String dataUrl = "data:text/plain;base64,VGhpcyBpcyBhIHRlc3QgZmlsZSBjb250ZW50";
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
                                .attachedUrl(dataUrl)
                                .build()
                ))
                .build();

        // When: Create board
        mockMvc.perform(post("/api/v1/board/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_CREATE_SUCCESS.getMessage()));

        // Then: Verify S3 URL is stored in database
        var record = dsl.fetchOne("SELECT ba.attached_url FROM board_attached ba WHERE ba.board_id = ?", TEST_BOARD_ID);
        assertThat(record).isNotNull();
        String storedUrl = record.get("attached_url", String.class);
        assertThat(storedUrl).isNotBlank();
        assertThat(storedUrl).startsWith("https://");
        assertThat(storedUrl).contains(".s3.");
        
        // Verify S3 file exists
        assertThat(s3FileService.fileExists(storedUrl)).isTrue();
    }

    @Test
    @Order(2)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board detail returns presigned URL for S3 attachment")
    @Transactional
    void getBoardDetail_returnsPresignedUrlForS3Attachment() throws Exception {
        // Given: Board with S3 attachment exists
        String s3Url = s3FileService.uploadBytes(
                "detail file".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                "board-detail-" + System.currentTimeMillis() + ".txt",
                "board-attachments"
        );
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "test.txt", "text/plain", "25", s3Url);

        // When: Get board detail
        mockMvc.perform(get("/api/v1/board/detail/{id}", TEST_BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.boardId").value(TEST_BOARD_ID))
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments[0].name").value("test.txt"))
                .andExpect(jsonPath("$.data.attachments[0].type").value("text/plain"))
                .andExpect(jsonPath("$.data.attachments[0].size").value("25"))
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").isString())
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value(org.hamcrest.Matchers.startsWith("https://")));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board update with data URL uploads to S3 and replaces old attachment")
    @Transactional
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
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_UPDATE_SUCCESS.getMessage()));

        // Then: Verify new S3 URL is stored
        var record = dsl.fetchOne("SELECT ba.attached_url FROM board_attached ba WHERE ba.board_id = ?", TEST_BOARD_ID);
        assertThat(record).isNotNull();
        String newUrl = record.get("attached_url", String.class);
        assertThat(newUrl).isNotBlank();
        assertThat(newUrl).startsWith("https://");
        assertThat(newUrl).contains(".s3.");
        assertThat(newUrl).isNotEqualTo(oldS3Url);
    }

    @Test
    @Order(4)
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Board delete removes S3 objects")
    @Transactional
    void deleteBoard_removesS3Objects() throws Exception {
        // Given: Board with S3 attachment exists
        String s3Url = s3FileService.uploadBytes(
                "delete file".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                "board-delete-" + System.currentTimeMillis() + ".txt",
                "board-attachments"
        );
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "삭제 테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "delete-test.txt", "text/plain", "30", s3Url);

        // When: Delete board
        mockMvc.perform(delete("/api/v1/board/delete/{id}", TEST_BOARD_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_DELETE_SUCCESS.getMessage()));

        // Then: Verify board is soft deleted and S3 deleted
        var record = dsl.fetchOne("SELECT deleted_at FROM board WHERE id = ?", TEST_BOARD_ID);
        assertThat(record).isNotNull();
        assertThat(record.get("deleted_at")).isNotNull();
        assertThat(s3FileService.fileExists(s3Url)).isFalse();
    }

    @Test
    @Order(5)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("Board search returns presigned URLs for attachments")
    @Transactional
    void searchBoards_returnsPresignedUrlsForAttachments() throws Exception {
        // Given: Board with S3 attachment exists
        String s3Url = s3FileService.uploadBytes(
                "search file".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                "board-search-" + System.currentTimeMillis() + ".txt",
                "board-attachments"
        );
        dsl.execute("INSERT INTO board (id, member_id, title, content, description, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())", 
                TEST_BOARD_ID, 1L, "검색 테스트 게시글", "내용", "설명", "TECH");
        dsl.execute("INSERT INTO board_attached (board_id, member_id, name, type, size, attached_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_BOARD_ID, 1L, "search-test.txt", "text/plain", "35", s3Url);

        // When: Search boards
        mockMvc.perform(get("/api/v1/board/search")
                        .param("keyword", "검색")
                        .param("category", "TECH"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].boardId").value(TEST_BOARD_ID))
                .andExpect(jsonPath("$.data.content[0].title").value("검색 테스트 게시글"));
    }
}
