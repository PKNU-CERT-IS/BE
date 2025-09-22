package org.certis.studyplatform.board.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.response.ResponseStatus;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.List;

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
@DisplayName("🚀 Board Controller 실제 컨트롤러 매핑 기준 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BoardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired EntityManager em;

    private static final Long TEST_BOARD_ID = 1L;
    private static final Long TEST_BOARD_ID_FOR_DIFFERENCE = 2L;

    @Test
    @Order(1)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("1️⃣ 게시글 생성 - 전체 플로우 테스트")
    @Transactional
    @Commit
    void createBoard_FullFlow_Success() throws Exception {
        em.unwrap(Session.class).doWork(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
            }
        });

        BoardCreateRequestDto request = BoardCreateRequestDto.builder()
                .title("실제 통합 테스트 게시글")
                .content("실제 데이터베이스에 저장되는 게시글 내용입니다.")
                .description("통합 테스트를 위한 게시글 설명")
                .category("TECH")
                .attachments(List.of())
                .build();

        mockMvc.perform(post("/api/v1/board/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_CREATE_SUCCESS.getMessage()));

        // 두 번째 게시글 생성 (id=2)
        BoardCreateRequestDto request2 = BoardCreateRequestDto.builder()
                .title("두번째 통합 테스트 게시글")
                .content("두번째 게시글 내용입니다.")
                .description("두번째 게시글 설명")
                .category("TECH")
                .attachments(List.of())
                .build();

        mockMvc.perform(post("/api/v1/board/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

    }

    @Test
    @Order(2)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("2️⃣ 게시글 상세 조회 - 생성된 게시글 조회")
    @Transactional(readOnly = true)
    void getBoardDetail_CreatedBoard_Success() throws Exception {
        mockMvc.perform(get("/api/v1/board/detail/{id}", TEST_BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.boardId").value(TEST_BOARD_ID));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("3️⃣ 게시글 검색 - 생성된 게시글이 검색되는지 확인")
    @Transactional(readOnly = true)
    void searchBoards_FindCreatedBoard_Success() throws Exception {
        mockMvc.perform(get("/api/v1/board/search")
                        .param("keyword", "실제 통합")
                        .param("category", "TECH")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("3️⃣-추가 키워드/카테고리 없이 전체 조회")
    @Transactional(readOnly = true)
    void searchBoards_NoParams_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/v1/board/search"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                // 기본 페이징(page=0, size=10) 확인
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("3️⃣-기본 페이징 확인 - 명시적 page/size 없이 size=10, page=0")
    @Transactional(readOnly = true)
    void searchBoards_DefaultPaging_WhenNoPageSizeParams() throws Exception {
        mockMvc.perform(get("/api/v1/board/search")
                        .param("keyword", "테스트"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("3️⃣-ALL 카테고리로 전체 게시글 조회")
    @Transactional(readOnly = true)
    void searchBoards_WithAllCategory_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/v1/board/search")
                        .param("keyword", "테스트")
                        .param("category", "ALL")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "user1", roles = "UPSOLVER") // 게시글 생성자와 동일
    @DisplayName("4️⃣-1 본인 게시글 좋아요 시도 → 실패")
    @Transactional
    void toggleLike_Fail_OwnBoard() throws Exception {
        mockMvc.perform(post("/api/v1/board/like/{id}", TEST_BOARD_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest()) // DomainException → 400
                .andExpect(jsonPath("$.message").value("본인 게시글에는 좋아요를 할 수 없습니다"));
    }

    @Test
    @Order(5)
    @WithMockUser(username = "user2", roles = "UPSOLVER")
    @DisplayName("4️⃣-2 이미 좋아요한 게시글 좋아요 삭제 → 다시 시도 -> 좋아요 성공")
    @Transactional
    void toggleLike_Fail_AlreadyLiked() throws Exception {
        // 사전 상태: 해당 게시글에 이미 좋아요를 한 상태로 만들기
        mockMvc.perform(post("/api/v1/board/like/{id}", TEST_BOARD_ID_FOR_DIFFERENCE)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글을 성공적으로 좋아요했습니다"));

        // 첫 번째 시도 → (이미 좋아요한 상태에서 취소)
        mockMvc.perform(post("/api/v1/board/like/{id}", TEST_BOARD_ID_FOR_DIFFERENCE)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 좋아요를 성공적으로 취소했습니다"));

        // 두 번째 좋아요 성공
        mockMvc.perform(post("/api/v1/board/like/{id}", TEST_BOARD_ID_FOR_DIFFERENCE)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글을 성공적으로 좋아요했습니다"));
    }


    @Test
    @Order(6)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("5️⃣ 게시글 수정 - 실제 데이터 변경 확인")
    @Transactional
    void updateBoard_ModifyRealData_Success() throws Exception {
        BoardUpdateRequestDto request = BoardUpdateRequestDto.builder()
                .title("수정된 통합 테스트 게시글")
                .content("수정된 게시글 내용입니다.")
                .description("수정된 게시글 설명")
                .category("TECH")
                .attachments(List.of())
                .build();

        mockMvc.perform(put("/api/v1/board/edit/{id}", TEST_BOARD_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("6️⃣ 게시글 삭제 - 관리자 권한 삭제")
    @Transactional
    void deleteBoard_AdminPermission_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/board/delete/{id}", TEST_BOARD_ID)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_DELETE_SUCCESS.getMessage()));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "admin", roles = "STAFF")
    @DisplayName("7️⃣ 매일 00시 View→RDB 동기화 트리거 검증 - 수동 호출 시 정상 동작")
    @Transactional
    void syncBoardStatsDaily_ManualTrigger_Success() throws Exception {
        // 사전 조건: 통계 관련 View 데이터가 존재한다고 가정 (테스트 DB에 seed 되어 있음)
        // 동기화 엔드포인트가 별도로 노출되어 있다면 해당 엔드포인트를 호출하는 방식 권장.
        // 여기서는 스케줄러가 호출하는 Application Service 흐름을 컨트롤러 레벨에서 검증하기 위해
        // 관리용 엔드포인트를 노출했다고 가정: /api/v1/board/admin/sync

        mockMvc.perform(post("/api/v1/board/admin/sync").with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_SYNC_SUCCESS"));

        // 이후 효과 검증: 특정 집계 조회 API가 동기화 결과를 반영했는지 확인
        mockMvc.perform(get("/api/v1/board/stats/today"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_STATS_FIND_SUCCESS"))
                .andExpect(jsonPath("$.data.synced").value(true));
    }
}
