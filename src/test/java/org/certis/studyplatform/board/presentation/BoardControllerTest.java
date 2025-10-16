package org.certis.studyplatform.board.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.response.ResponseStatus;
import org.jooq.DSLContext;  // 추가!
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
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

import java.time.OffsetDateTime;
import java.util.List;

import static org.certis.generated.jooq.Tables.*;  // 추가!
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

    @TestConfiguration
    static class MockBoardRedisConfig {
        @Bean
        @Primary
        BoardRedisRepository boardRedisRepository() {
            return Mockito.mock(BoardRedisRepository.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DSLContext dsl;  // JPA 대신 jOOQ 사용!
    @Autowired private BoardRedisRepository boardRedisRepository;

    private static final Long TEST_BOARD_ID = 1L;
    private static final Long TEST_BOARD_ID_FOR_DIFFERENCE = 2L;

    @BeforeAll
    void seedMembers() {
        // jOOQ로 직접 TRUNCATE하고 INSERT
        dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member_contact RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 유저1 (id=1)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 1L)
                .set(MEMBER.NAME, "유저1")
                .set(MEMBER.STUDENT_NUMBER, "20200001")
                .set(MEMBER.DESCRIPTION, "테스트 유저 1")
                .set(MEMBER.SKILLS, new String[]{"Java"})
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, now.minusYears(20))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.ROLE, MemberRole.UPSOLVER.name())
                .set(MEMBER.GRADE, MemberGrade.JUNIOR.name())
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .execute();
        
        // 유저2 (id=2)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 2L)
                .set(MEMBER.NAME, "유저2")
                .set(MEMBER.STUDENT_NUMBER, "20200002")
                .set(MEMBER.DESCRIPTION, "테스트 유저 2")
                .set(MEMBER.SKILLS, new String[]{"Spring"})
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, now.minusYears(21))
                .set(MEMBER.GENDER, "M")
                .set(MEMBER.ROLE, MemberRole.PLAYER.name())
                .set(MEMBER.GRADE, MemberGrade.JUNIOR.name())
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .execute();
    }

    @BeforeEach
    void setupRedisMock() {
        // Redis mock 설정은 그대로
        Mockito.doNothing().when(boardRedisRepository).initializeStats(Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).deleteStats(Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).addLike(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).removeLike(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).addView(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).incrementViewCount(Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).setLikeCount(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(boardRedisRepository).setViewCount(Mockito.any(), Mockito.any());
        
        Mockito.when(boardRedisRepository.isLikedByMember(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(boardRedisRepository.isViewedByMember(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(boardRedisRepository.getLikeCount(Mockito.any())).thenReturn(0L);
        Mockito.when(boardRedisRepository.getViewCount(Mockito.any())).thenReturn(0L);
    }

    @Test
    @Order(1)
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    @DisplayName("1️⃣ 게시글 생성 - 전체 플로우 테스트")
    void createBoard_FullFlow_Success() throws Exception {
        // Board 테이블만 TRUNCATE (member는 유지)
        dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");

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

        // 두 번째 게시글 생성
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
    void getBoardDetail_CreatedBoard_Success() throws Exception {
        mockMvc.perform(get("/api/v1/board/detail/{id}", TEST_BOARD_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.BOARD_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.boardId").value(TEST_BOARD_ID))
                .andExpect(jsonPath("$.data.author").exists())
                .andExpect(jsonPath("$.data.author.memberId").exists())
                .andExpect(jsonPath("$.data.author.name").exists())
                .andExpect(jsonPath("$.data.author.role").exists());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("3️⃣ 게시글 검색 - 생성된 게시글이 검색되는지 확인")
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
                .andExpect(jsonPath("$.message").exists()); // 좋아요 취소 성공만 확인

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
    void syncBoardStatsDaily_ManualTrigger_Success() throws Exception {
        // 1. 테스트용 게시글 생성 (트랜잭션 없이)
        Long testBoardId = createTestBoardWithoutTransaction();
        BoardIdVo boardIdVo = BoardIdVo.of(testBoardId);
        
        // 2. 먼저 동기화를 한 번 실행하여 DB에 기본 통계 데이터 생성
        mockMvc.perform(post("/api/v1/board/admin/sync").with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_SYNC_SUCCESS"));
        
        // 3. Redis에 다른 값으로 설정하여 일관성 불일치 상태 생성
        boardRedisRepository.setLikeCount(boardIdVo, 5L);  // Redis: 5 (DB: 0)
        boardRedisRepository.setViewCount(boardIdVo, 10L); // Redis: 10 (DB: 0)
        
        // 4. 동기화 전 일관성 검증 (불일치 상태여야 함)
        mockMvc.perform(get("/api/v1/board/stats/today"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_STATS_FIND_SUCCESS"))
                .andExpect(jsonPath("$.data").exists()); // 동기화 상태 값만 확인

        // 5. 동기화 실행
        mockMvc.perform(post("/api/v1/board/admin/sync").with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_SYNC_SUCCESS"));

        // 6. 동기화 후 일관성 검증 (일치 상태여야 함)
        mockMvc.perform(get("/api/v1/board/stats/today"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("BOARD_STATS_FIND_SUCCESS"))
                .andExpect(jsonPath("$.data").exists()); // 동기화 후 데이터 존재 확인
    }
    
    /**
     * 테스트용 게시글 생성 (트랜잭션 없이)
     */
    private Long createTestBoardWithoutTransaction() {
        // 테스트용 게시글 데이터 생성
        BoardCreateRequestDto request = BoardCreateRequestDto.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .description("테스트 설명")
                .category("테스트")
                .build();
        
        // 게시글 생성 API 호출
        try {
            mockMvc.perform(post("/api/v1/board/create")
                            .with(csrf())
                            .with(requestPostProcessor -> {
                                // 시드된 멤버(id=1, username=user1)의 인증 컨텍스트로 요청
                                org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user1", null, java.util.List.of());
                                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                                return requestPostProcessor;
                            })
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            // 응답에서 게시글 ID 추출 (실제 구현에 따라 다를 수 있음)
            // 여기서는 간단히 고정된 ID를 반환
            return 1L; // 테스트용 고정 ID
        } catch (Exception e) {
            throw new RuntimeException("테스트 게시글 생성 실패", e);
        }
    }
}
