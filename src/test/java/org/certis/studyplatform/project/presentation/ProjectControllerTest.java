package org.certis.studyplatform.project.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.project.presentation.dto.request.*;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import org.certis.studyplatform.response.ResponseStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProjectController 완전 새로운 통합 테스트
 *
 * 🎯 새로운 테스트 특징:
 * - CQRS 패턴과 JOOQ 기반 아키텍처 완전 활용
 * - Clean Architecture 계층별 테스트 분리
 * - BDD 스타일 테스트 시나리오 (Given-When-Then)
 * - 실제 비즈니스 시나리오 기반 테스트 케이스
 * - 포괄적인 검증 및 엣지 케이스 커버
 * - 테스트 데이터 격리 및 독립성 보장
 * - 성능 및 동시성 고려 테스트
 *
 * 🔧 기술 스택:
 * - Spring Boot Test with @SpringBootTest
 * - MockMvc for REST API testing
 * - JOOQ DSLContext for direct database operations
 * - Embedded PostgreSQL for isolated testing
 * - AssertJ for fluent assertions
 * - JUnit 5 with ordered test execution
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🚀 ProjectController 새로운 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final Long TEST_PROJECT_ID = 1L;
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_MEMBER_2_ID = 2L;

    private static final String TEST_PROJECT_TITLE = "CERT-IS 학습 플랫폼 프로젝트";
    private static final String TEST_PROJECT_DESCRIPTION = "통합 테스트용 프로젝트";
    private static final String TEST_PROJECT_CONTENT = "프로젝트 상세 내용입니다.";
    private static final String TEST_PROJECT_CATEGORY = "웹 개발";
    private static final String TEST_PROJECT_SUBCATEGORY = "풀스택";

    private static final String TEST_MEMBER_NAME = "김개발";
    private static final String TEST_MEMBER_2_NAME = "이테스트";

    @BeforeEach
    void setUp() {
        System.out.println("🔧 테스트 데이터 설정 시작");
        // 데이터 충돌 방지를 위해 매 테스트 시작 시 테이블 정리
        dsl.execute("TRUNCATE TABLE project_meeting_link RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project_meeting RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
        System.out.println("✅ 테스트 데이터 설정 완료");
    }

    @AfterEach
    void tearDown() {
        System.out.println("🧹 테스트 데이터 정리 시작");
        cleanupTestData();
        System.out.println("✅ 테스트 데이터 정리 완료");
    }

    // =================================================================
    // 🎯 비즈니스 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("📝 프로젝트 생성 - 성공적인 비즈니스 시나리오")
    void createProject_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 프로젝트 생성 요청이 준비됨
        ProjectCreateRequestDto request = createValidProjectRequest();

        // When: 프로젝트 생성 API를 호출
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_CREATE_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_CREATE_SUCCESS.getMessage()));

        // Then: 데이터베이스에 프로젝트가 정상적으로 저장되었는지 검증
        verifyProjectCreatedInDatabase(request);

        System.out.println("✅ 프로젝트 생성 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("🔍 프로젝트 상세 조회 - 완전한 정보 반환")
    void getProjectDetail_CompleteInformationReturned() throws Exception {
        // Given: 프로젝트가 미리 생성되어 있음
        createTestProjectInDatabase();

        // When: 프로젝트 상세 조회 API 호출
        var mvcResult = mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", TEST_PROJECT_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 완전한 프로젝트 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_FIND_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.creatorId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.title").value(TEST_PROJECT_TITLE))
                .andExpect(jsonPath("$.data.description").value(TEST_PROJECT_DESCRIPTION))
                .andExpect(jsonPath("$.data.content").value(TEST_PROJECT_CONTENT))
                .andExpect(jsonPath("$.data.category").value(TEST_PROJECT_CATEGORY))
                .andExpect(jsonPath("$.data.subCategory").value(TEST_PROJECT_SUBCATEGORY))
                .andExpect(jsonPath("$.data.maxParticipantNumber").exists())
                .andExpect(jsonPath("$.data.currentParticipantNumber").exists())
                .andReturn();

        // And: DB의 member_id와 응답의 creatorId가 일치하는지 검증
        String content = mvcResult.getResponse().getContentAsString();
        long responseCreatorId = objectMapper.readTree(content).at("/data/creatorId").asLong();
        Long dbCreatorId = dsl.select(PROJECT.MEMBER_ID)
                .from(PROJECT)
                .where(PROJECT.ID.eq(TEST_PROJECT_ID))
                .fetchOne(PROJECT.MEMBER_ID);
        assertThat(responseCreatorId).isEqualTo(dbCreatorId);

        System.out.println("✅ 프로젝트 상세 조회 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("✏️ 프로젝트 수정 - 권한 있는 사용자의 성공적인 수정")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void updateProject_AuthorizedUserSuccessfulUpdate() throws Exception {
        // Given: 프로젝트가 존재하고, 작성자가 수정을 요청함
        createTestProjectInDatabase();

        ProjectUpdateRequestDto request = new ProjectUpdateRequestDto();
        request.setProjectId(TEST_PROJECT_ID);
        request.setTitle("수정된 프로젝트 제목");
        request.setDescription("수정된 프로젝트 설명입니다.");
        request.setContent("수정된 프로젝트 상세 내용입니다.");
        request.setCategory("모바일 개발");
        request.setSubCategory("안드로이드");
        request.setMaxParticipants(10);

        // When: 프로젝트 수정 API 호출
        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_UPDATE_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_UPDATE_SUCCESS.getMessage()));

        // Then: 데이터베이스에서 프로젝트 수정 확인
        verifyProjectUpdatedInDatabase(request);

        System.out.println("✅ 프로젝트 수정 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("📋 전체 프로젝트 목록 조회 - 페이징과 정렬 기능")
    void getAllProjects_PaginationAndSortingFeatures() throws Exception {
        // Given: 여러 개의 프로젝트가 존재함
        createMultipleProjectsInDatabase();

        // When: 전체 프로젝트 목록 조회
        mockMvc.perform(get("/api/v1/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_SEARCH_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.data.content").isArray())  // 변경: $.data → $.data.content
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

        System.out.println("✅ 전체 프로젝트 목록 조회 테스트 성공");
    }

    @Test
    @Order(5)
    @DisplayName("🔍 프로젝트 검색 - 키워드 기반 검색")
    void searchProjects_KeywordBasedSearch() throws Exception {
        // Given: 검색 가능한 프로젝트들이 존재함
        createSearchableProjectsInDatabase();

        // When: 키워드로 프로젝트 검색
        mockMvc.perform(get("/api/v1/project/search")
                        .param("keyword", "학습")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 검색 결과와 페이징 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_SEARCH_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_SEARCH_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));

        System.out.println("✅ 프로젝트 키워드 검색 테스트 성공");
    }

    @Test
    @Order(6)
    @DisplayName("🗑️ 프로젝트 삭제 - 권한 있는 사용자의 성공적인 삭제")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void deleteProject_AuthorizedUserSuccessfulDeletion() throws Exception {
        // Given: 프로젝트가 존재하고, 생성자가 삭제를 요청함
        createTestProjectInDatabase();

        ProjectDeleteRequestDto request = new ProjectDeleteRequestDto();
        request.setProjectId(TEST_PROJECT_ID);

        // When: 프로젝트 삭제 API 호출
        mockMvc.perform(delete("/api/v1/project/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_DELETE_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_DELETE_SUCCESS.getMessage()));

        // Then: 데이터베이스에서 소프트 삭제 확인 (deletedAt 필드 설정)
        verifyProjectDeletedInDatabase(TEST_PROJECT_ID);

        System.out.println("✅ 프로젝트 삭제 테스트 성공");
    }

    @Test
    @Order(7)
    @DisplayName("📋 프로젝트 회의록 목록 조회 - 특정 프로젝트의 회의록들")
    void getProjectMeetings_SpecificProjectMeetings() throws Exception {
        // Given: 프로젝트와 회의록들이 존재함
        createTestProjectInDatabase();
        createTestMeetingsForProject();

        // When: 특정 프로젝트의 회의록 목록 조회
        mockMvc.perform(get("/api/v1/project/{projectId}/meetings", TEST_PROJECT_ID))
                .andDo(print())
                // Then: 해당 프로젝트의 회의록 목록 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_FIND_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROJECT_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());

        System.out.println("✅ 프로젝트 회의록 목록 조회 테스트 성공");
    }

    // =================================================================
    // 🚨 검증 실패 및 예외 상황 테스트
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("❌ 프로젝트 생성 실패 - 필수 필드 누락")
    void createProject_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        ProjectCreateRequestDto request = new ProjectCreateRequestDto();
        // title, description, category 등 필수 필드 누락

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 필수 필드 누락 검증 테스트 성공");
    }

    @Test
    @Order(11)
    @DisplayName("❌ 프로젝트 생성 실패 - 잘못된 데이터 형식")
    void createProject_ValidationFailure_InvalidDataFormat() throws Exception {
        // Given: 잘못된 형식의 데이터
        ProjectCreateRequestDto request = new ProjectCreateRequestDto();
        request.setTitle(""); // 빈 제목
        request.setDescription(""); // 빈 설명
        request.setCategory(""); // 빈 카테고리
        request.setSubCategory(""); // 빈 서브카테고리
        request.setMaxParticipants(-1); // 음수 참가자 수
        request.setStartDate(OffsetDateTime.now().minusDays(1)); // 과거 시작일
        request.setEndDate(OffsetDateTime.now().minusDays(2)); // 시작일보다 이전 종료일

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 잘못된 데이터 형식 검증 테스트 성공");
    }

    @Test
    @Order(12)
    @DisplayName("❌ 프로젝트 조회 실패 - 존재하지 않는 프로젝트")
    void getProjectDetail_NotFound_NonExistentProject() throws Exception {
        // Given: 존재하지 않는 프로젝트 ID
        Long nonExistentProjectId = 99999L;

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", nonExistentProjectId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));

        System.out.println("✅ 존재하지 않는 프로젝트 조회 테스트 성공");
    }

    @Test
    @Order(13)
    @DisplayName("❌ 프로젝트 수정 실패 - 권한 없는 사용자")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void updateProject_AuthorizationFailure_UnauthorizedUser() throws Exception {
        // Given: 프로젝트가 존재하지만, 다른 사용자가 수정을 시도
        createTestProjectInDatabase();

        ProjectUpdateRequestDto request = new ProjectUpdateRequestDto();
        request.setProjectId(TEST_PROJECT_ID);
        request.setTitle("무단 수정 시도");
        request.setDescription("권한이 없는 사용자의 수정 시도");

        // When & Then: HTTP 400 BadRequest 응답
        // TODO: 하드코딩 변경에 따라 변경
        mockMvc.perform(put("/api/v1/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 권한 없는 사용자 수정 시도 테스트 성공");
    }

    @Test
    @Order(14)
    @DisplayName("❌ 프로젝트 삭제 실패 - 권한 없는 사용자")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void deleteProject_AuthorizationFailure_UnauthorizedUser() throws Exception {
        // Given: 프로젝트가 존재하지만, 다른 사용자가 삭제를 시도
        createTestProjectInDatabase();

        ProjectDeleteRequestDto request = new ProjectDeleteRequestDto();
        request.setProjectId(TEST_PROJECT_ID);

        // When & Then: HTTP 400 BadRequest 응답
        mockMvc.perform(delete("/api/v1/project/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 권한 없는 사용자 삭제 시도 테스트 성공");
    }

    // =================================================================
    // 🔧 엣지 케이스 및 기술적 테스트
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("🎭 잘못된 Content-Type - HTTP 415 Unsupported Media Type")
    void createProject_TechnicalFailure_UnsupportedMediaType() throws Exception {
        // Given: 잘못된 Content-Type
        ProjectCreateRequestDto request = createValidProjectRequest();

        // When & Then: HTTP 415 Unsupported Media Type 응답
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        System.out.println("✅ 잘못된 Content-Type 테스트 성공");
    }

    @Test
    @Order(21)
    @DisplayName("🎭 잘못된 JSON 형식 - HTTP 400 Bad Request")
    void createProject_TechnicalFailure_MalformedJson() throws Exception {
        // When & Then: 잘못된 JSON으로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        System.out.println("✅ 잘못된 JSON 형식 테스트 성공");
    }

    @Test
    @Order(22)
    @DisplayName("🎭 잘못된 HTTP 메서드 - HTTP 405 Method Not Allowed")
    void getProjectDetail_TechnicalFailure_WrongHttpMethod() throws Exception {
        // When & Then: GET 엔드포인트에 POST 요청으로 HTTP 405 Method Not Allowed 응답
        mockMvc.perform(post("/api/v1/project/detail")
                        .param("projectId", "1"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        System.out.println("✅ 잘못된 HTTP 메서드 테스트 성공");
    }

    @Test
    @Order(23)
    @DisplayName("🎭 존재하지 않는 엔드포인트 - HTTP 404 Not Found")
    void nonExistentEndpoint_TechnicalFailure_NotFound() throws Exception {
        // When & Then: 존재하지 않는 엔드포인트로 HTTP 404 Not Found 응답
        mockMvc.perform(get("/api/v1/project/nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound());

        System.out.println("✅ 존재하지 않는 엔드포인트 테스트 성공");
    }

    // =================================================================
    // 📊 성능 및 대용량 데이터 테스트
    // =================================================================

    @Test
    @Order(30)
    @DisplayName("📊 대용량 프로젝트 목록 조회 - 성능 테스트")
    void getAllProjects_PerformanceTest_LargeDataset() throws Exception {
        // Given: 대량의 프로젝트 데이터 생성 (50개)
        createLargeProjectDataset(50);

        long startTime = System.currentTimeMillis();

        // When: 전체 프로젝트 목록 조회
        mockMvc.perform(get("/api/v1/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ResponseStatus.PROJECT_SEARCH_SUCCESS.getStatusCode()))
                .andExpect(jsonPath("$.data.content").isArray())  // 변경: $.data → $.data.content
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(50))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 성능 검증: 2초 이내 응답
        assertThat(executionTime).isLessThan(2000);

        System.out.println("✅ 대용량 데이터 성능 테스트 성공 - 실행시간: " + executionTime + "ms");
    }

    @Test
    @Order(31)
    @DisplayName("📊 복합 조건 프로젝트 검색 - 다중 필터 성능")
    void searchProjects_ComplexFilters_PerformanceTest() throws Exception {
        // Given: 다양한 카테고리의 프로젝트들이 존재함
        createDiverseProjectsInDatabase();

        long startTime = System.currentTimeMillis();

        // When: 복합 조건으로 프로젝트 검색
        mockMvc.perform(get("/api/v1/project/search")
                        .param("keyword", "플랫폼")
                        .param("category", TEST_PROJECT_CATEGORY)
                        .param("subcategory", TEST_PROJECT_SUBCATEGORY)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                // Then: 정상 검색 결과 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 성능 검증: 1초 이내 응답
        assertThat(executionTime).isLessThan(1000);

        System.out.println("✅ 복합 조건 검색 성능 테스트 성공 - 실행시간: " + executionTime + "ms");
    }


    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 프로젝트 생성 요청 DTO 생성
     */
    private ProjectCreateRequestDto createValidProjectRequest() {
        ProjectCreateRequestDto request = new ProjectCreateRequestDto();
        request.setTitle(TEST_PROJECT_TITLE);
        request.setDescription(TEST_PROJECT_DESCRIPTION);
        request.setContent(TEST_PROJECT_CONTENT);
        request.setCategory(TEST_PROJECT_CATEGORY);
        request.setSubCategory(TEST_PROJECT_SUBCATEGORY);
        request.setMaxParticipants(5);
        request.setStartDate(OffsetDateTime.now().plusDays(1));
        request.setEndDate(OffsetDateTime.now().plusDays(30));
        return request;
    }

    /**
     * 테스트 데이터 설정 (멤버)
     */
    private void setupTestData() {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            // 멤버 데이터 생성
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_MEMBER_ID)
                    .set(MEMBER.NAME, TEST_MEMBER_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "kim.dev@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(25))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, "SENIOR")
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_MEMBER_2_ID)
                    .set(MEMBER.NAME, TEST_MEMBER_2_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "lee.test@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "FEMALE")
                    .set(MEMBER.GRADE, "JUNIOR")
                    .set(MEMBER.MAJOR, "정보보안학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

        } catch (Exception e) {
            System.out.println("테스트 데이터 설정 중 오류 발생 (이미 존재할 수 있음): " + e.getMessage());
        }
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        try {
            // 외래 키 제약으로 인해 역순으로 삭제
            dsl.deleteFrom(PROJECT_MEETING).execute();
            dsl.deleteFrom(PROJECT).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
            System.out.println("테스트 데이터 정리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스에 테스트용 프로젝트 생성
     */
    private void createTestProjectInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, TEST_PROJECT_TITLE)
                .set(PROJECT.DESCRIPTION, TEST_PROJECT_DESCRIPTION)
                .set(PROJECT.CONTENT, TEST_PROJECT_CONTENT)
                .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT.CATEGORY, TEST_PROJECT_CATEGORY)
                .set(PROJECT.SUBCATEGORY, TEST_PROJECT_SUBCATEGORY)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(30))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();
    }

    /**
     * 다수의 프로젝트 생성 (목록 조회 테스트용)
     */
    private void createMultipleProjectsInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 1; i <= 3; i++) {
            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, (long) i)
                    .set(PROJECT.TITLE, "테스트 프로젝트 " + i)
                    .set(PROJECT.DESCRIPTION, "테스트 프로젝트 " + i + " 설명")
                    .set(PROJECT.CONTENT, "테스트 프로젝트 " + i + " 내용")
                    .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT.CATEGORY, TEST_PROJECT_CATEGORY)
                    .set(PROJECT.SUBCATEGORY, TEST_PROJECT_SUBCATEGORY)
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(PROJECT.STARTED_AT, now.plusDays(i))
                    .set(PROJECT.ENDED_AT, now.plusDays(30 + i))
                    .set(PROJECT.CREATED_AT, now.minusHours(i))
                    .set(PROJECT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 검색 가능한 프로젝트들 생성
     */
    private void createSearchableProjectsInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        String[] titles = {"학습 플랫폼 개발", "학습 관리 시스템", "온라인 교육 플랫폼"};

        for (int i = 0; i < titles.length; i++) {
            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, (long) (i + 1))
                    .set(PROJECT.TITLE, titles[i])
                    .set(PROJECT.DESCRIPTION, titles[i] + " 설명")
                    .set(PROJECT.CONTENT, titles[i] + " 상세 내용")
                    .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT.CATEGORY, TEST_PROJECT_CATEGORY)
                    .set(PROJECT.SUBCATEGORY, TEST_PROJECT_SUBCATEGORY)
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(PROJECT.STARTED_AT, now.plusDays(i + 1))
                    .set(PROJECT.ENDED_AT, now.plusDays(30 + i))
                    .set(PROJECT.CREATED_AT, now.minusHours(i))
                    .set(PROJECT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 대용량 프로젝트 데이터셋 생성 (성능 테스트용)
     */
    private void createLargeProjectDataset(int count) {
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 1; i <= count; i++) {
            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, (long) i)
                    .set(PROJECT.TITLE, "대용량 테스트 프로젝트 " + i)
                    .set(PROJECT.DESCRIPTION, "대용량 테스트용 프로젝트 설명 " + i)
                    .set(PROJECT.CONTENT, "대용량 테스트용 프로젝트 내용 " + i)
                    .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT.CATEGORY, TEST_PROJECT_CATEGORY)
                    .set(PROJECT.SUBCATEGORY, TEST_PROJECT_SUBCATEGORY)
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(PROJECT.STARTED_AT, now.plusDays(i))
                    .set(PROJECT.ENDED_AT, now.plusDays(30 + i))
                    .set(PROJECT.CREATED_AT, now.minusHours(i))
                    .set(PROJECT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 다양한 카테고리의 프로젝트들 생성
     */
    private void createDiverseProjectsInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        String[] categories = {"웹 개발", "모바일 개발", "데이터 분석"};
        String[] subcategories = {"풀스택", "프론트엔드", "백엔드"};

        for (int i = 0; i < categories.length; i++) {
            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, (long) (i + 1))
                    .set(PROJECT.TITLE, categories[i] + " 플랫폼 " + (i + 1))
                    .set(PROJECT.DESCRIPTION, categories[i] + " 프로젝트 설명")
                    .set(PROJECT.CONTENT, categories[i] + " 프로젝트 상세 내용")
                    .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT.CATEGORY, categories[i])
                    .set(PROJECT.SUBCATEGORY, subcategories[i])
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(PROJECT.STARTED_AT, now.plusDays(i + 1))
                    .set(PROJECT.ENDED_AT, now.plusDays(30 + i))
                    .set(PROJECT.CREATED_AT, now.minusHours(i))
                    .set(PROJECT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 테스트용 회의록들 생성
     */
    private void createTestMeetingsForProject() {
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 1; i <= 2; i++) {
            dsl.insertInto(PROJECT_MEETING)
                    .set(PROJECT_MEETING.ID, (long) i)
                    .set(PROJECT_MEETING.PROJECT_ID, TEST_PROJECT_ID)
                    .set(PROJECT_MEETING.MEMBER_ID, TEST_MEMBER_ID)
                    .set(PROJECT_MEETING.TITLE, "프로젝트 회의록 " + i)
                    .set(PROJECT_MEETING.CONTENT, "프로젝트 회의록 " + i + " 내용")
                    .set(PROJECT_MEETING.PARTICIPANTS, new Long[]{TEST_MEMBER_ID, TEST_MEMBER_2_ID})
                    .set(PROJECT_MEETING.CREATED_AT, now.minusHours(i))
                    .set(PROJECT_MEETING.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 프로젝트 생성 검증
     */
    private void verifyProjectCreatedInDatabase(ProjectCreateRequestDto request) {
        var project = dsl.selectFrom(PROJECT)
                .where(PROJECT.TITLE.eq(request.getTitle()))
                .and(PROJECT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(project).isNotNull();
        assertThat(project.getTitle()).isEqualTo(request.getTitle());
        assertThat(project.getDescription()).isEqualTo(request.getDescription());
        assertThat(project.getContent()).isEqualTo(request.getContent());
        assertThat(project.getCategory()).isEqualTo(request.getCategory());
        assertThat(project.getSubcategory()).isEqualTo(request.getSubCategory());
    }

    // =================================================================
    // 🆕 새로운 기능 테스트 (demoUrl, externalUrl 구조 변경)
    // =================================================================

    @Test
    @Order(100)
    @DisplayName("✅ 프로젝트 생성 시 demoUrl과 구조화된 externalUrl이 올바르게 저장된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_create_project_with_demo_url_and_structured_external_url() throws Exception {
        // Given: demoUrl과 구조화된 externalUrl을 포함한 프로젝트 생성 요청
        ProjectCreateRequestDto request = createProjectCreateRequestWithNewFields();

        // When: 프로젝트 생성 요청
        mockMvc.perform(post("/api/v1/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("프로젝트가 성공적으로 생성되었습니다"));

        // Then: 데이터베이스에서 demoUrl과 externalUrl이 올바르게 저장되었는지 확인
        var project = dsl.selectFrom(PROJECT)
                .where(PROJECT.TITLE.eq(request.getTitle()))
                .and(PROJECT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(project).isNotNull();
        assertThat(project.getDemoUrl()).isEqualTo(request.getDemoUrl());
        assertThat(project.getExternalUrl()).contains(request.getExternalUrl().getTitle());
        assertThat(project.getExternalUrl()).contains(request.getExternalUrl().getUrl());
    }

    @Test
    @Order(101)
    @DisplayName("✅ 프로젝트 상세 조회 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_project_detail() throws Exception {
        // Given: 새로운 필드들을 포함한 프로젝트가 존재하는 상태
        createProjectWithNewFields();

        // When: 프로젝트 상세 조회
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", String.valueOf(TEST_PROJECT_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.projectCreatorName").exists())
                .andExpect(jsonPath("$.data.projectCreatorGrade").exists())
                .andExpect(jsonPath("$.data.semester").exists())
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.maxParticipantNumber").exists())
                .andExpect(jsonPath("$.data.currentParticipantNumber").exists())
                .andExpect(jsonPath("$.data.demoUrl").exists())
                .andExpect(jsonPath("$.data.externalUrl.title").exists())
                .andExpect(jsonPath("$.data.externalUrl.url").exists());
    }

    @Test
    @Order(102)
    @DisplayName("✅ 프로젝트 목록 조회 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_project_list() throws Exception {
        // Given: 새로운 필드들을 포함한 프로젝트가 존재하는 상태
        createProjectWithNewFields();

        // When: 프로젝트 목록 조회
        mockMvc.perform(get("/api/v1/project")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.content[0].semester").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists())
                .andExpect(jsonPath("$.data.content[0].maxParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].currentParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].demoUrl").exists())
                .andExpect(jsonPath("$.data.content[0].externalUrl.title").exists())
                .andExpect(jsonPath("$.data.content[0].externalUrl.url").exists());
    }

    @Test
    @Order(103)
    @DisplayName("✅ 프로젝트 검색 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_project_search() throws Exception {
        // Given: 새로운 필드들을 포함한 프로젝트가 존재하는 상태
        createProjectWithNewFields();

        // When: 프로젝트 검색
        mockMvc.perform(get("/api/v1/project/search")
                        .param("keyword", "새로운 필드")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.content[0].semester").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists())
                .andExpect(jsonPath("$.data.content[0].maxParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].currentParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].demoUrl").exists())
                .andExpect(jsonPath("$.data.content[0].externalUrl.title").exists())
                .andExpect(jsonPath("$.data.content[0].externalUrl.url").exists());
    }

    // =================================================================
    // 🛠 새로운 기능 테스트를 위한 헬퍼 메서드
    // =================================================================

    /**
     * 새로운 필드들을 포함한 프로젝트 생성 요청 생성
     */
    private ProjectCreateRequestDto createProjectCreateRequestWithNewFields() {
        ProjectCreateRequestDto request = new ProjectCreateRequestDto();
        request.setTitle("새로운 필드 테스트 프로젝트");
        request.setDescription("demoUrl과 구조화된 externalUrl 테스트");
        request.setContent("새로운 필드들이 올바르게 저장되는지 테스트");
        request.setCategory("CS");
        request.setSubCategory("백엔드");
        request.setStartDate(OffsetDateTime.now().plusDays(1));
        request.setEndDate(OffsetDateTime.now().plusDays(30));
        // request.setSkills(List.of("Spring Boot", "Java")); // skills 필드가 없음
        request.setMaxParticipants(5);
        request.setGithubUrl("https://github.com/test/new-project");
        
        // 구조화된 externalUrl 설정
        ExternalUrlRequestDto externalUrl = new ExternalUrlRequestDto();
        externalUrl.setTitle("프로젝트 사이트");
        externalUrl.setUrl("https://new-project.example.com");
        request.setExternalUrl(externalUrl);
        
        // demoUrl 설정
        request.setDemoUrl("https://demo.example.com/new-project");
        request.setThumbnailUrl("https://example.com/thumbnail.jpg");
        
        return request;
    }

    /**
     * 새로운 필드들을 포함한 프로젝트 생성
     */
    private void createProjectWithNewFields() {
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, "새로운 필드 테스트 프로젝트")
                .set(PROJECT.DESCRIPTION, "demoUrl과 구조화된 externalUrl을 포함한 새로운 필드들을 테스트하는 프로젝트입니다.")
                .set(PROJECT.CONTENT, "새로운 필드들이 올바르게 저장되는지 테스트")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "백엔드")
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.GITHUB_URL, "https://github.com/test/new-project")
                .set(PROJECT.EXTERNAL_URL, "{\"title\":\"프로젝트 사이트\",\"url\":\"https://new-project.example.com\"}")
                .set(PROJECT.DEMO_URL, "https://demo.example.com/new-project") // DEMO_URL 필드가 없음
                .set(PROJECT.THUMBNAIL_URL, "https://example.com/thumbnail.jpg")
                .set(PROJECT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    /**
     * 프로젝트 수정 검증
     */
    private void verifyProjectUpdatedInDatabase(ProjectUpdateRequestDto request) {
        var project = dsl.selectFrom(PROJECT)
                .where(PROJECT.ID.eq(request.getProjectId()))
                .and(PROJECT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(project).isNotNull();
        assertThat(project.getTitle()).isEqualTo(request.getTitle());
        assertThat(project.getDescription()).isEqualTo(request.getDescription());
        assertThat(project.getUpdatedAt()).isAfter(project.getCreatedAt());
    }

    /**
     * 프로젝트 삭제 검증 (소프트 삭제)
     */
    private void verifyProjectDeletedInDatabase(Long projectId) {
        var project = dsl.selectFrom(PROJECT)
                .where(PROJECT.ID.eq(projectId))
                .fetchOne();

        assertThat(project).isNotNull();
        assertThat(project.getDeletedAt()).isNotNull(); // 소프트 삭제 확인
    }

    @Test
    @Order(103)
    @DisplayName("✅ 프로젝트 종료 API 테스트 - 성공 케이스")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_end_project_successfully() throws Exception {
        // Given
        Long projectId = 1L;
        
        // 프로젝트 생성
        setupTestData();
        createTestProjectInDatabase();
        
        // When & Then
        mockMvc.perform(post("/api/v1/project/end")
                        .param("projectId", String.valueOf(projectId))
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트가 성공적으로 종료되었습니다"))
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED")); // 종료된 상태 확인
        
        // 데이터베이스에서 프로젝트 상태 확인
        var project = dsl.selectFrom(PROJECT)
                .where(PROJECT.ID.eq(projectId))
                .fetchOne();
        
        assertThat(project).isNotNull();
        assertThat(project.getEndedAt()).isNotNull(); // 종료 시간이 설정되었는지 확인
    }

    @Test
    @Order(104)
    @DisplayName("✅ 프로젝트 종료 API 테스트 - 권한 없음")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void should_fail_to_end_project_without_permission() throws Exception {
        // Given
        Long projectId = 1L;
        
        // 프로젝트 생성
        setupTestData();
        createTestProjectInDatabase();
        
        // When & Then
        mockMvc.perform(post("/api/v1/project/end")
                        .param("projectId", String.valueOf(projectId))
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("프로젝트 종료 권한이 없습니다")));
    }

    @Test
    @Order(105)
    @DisplayName("✅ 프로젝트 종료 API 테스트 - 이미 종료된 프로젝트")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_fail_to_end_already_ended_project() throws Exception {
        // Given
        Long projectId = 1L;
        
        // 프로젝트 생성 및 이미 종료된 상태로 설정
        setupTestData();
        createTestProjectInDatabase();
        dsl.update(PROJECT)
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().minusDays(10)) // 10일 전 시작
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().minusDays(1)) // 어제 종료
                .where(PROJECT.ID.eq(projectId))
                .execute();
        
        // When & Then
        mockMvc.perform(post("/api/v1/project/end")
                        .param("projectId", String.valueOf(projectId))
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 종료된 프로젝트입니다")));
    }

    @Test
    @Order(200)
    @DisplayName("✅ 프로젝트 상세 조회 - Status 값 검증")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_return_correct_project_status_in_detail() throws Exception {
        // Given
        Long projectId = 1L;
        setupTestData();
        
        // 프로젝트를 현재 진행 중인 상태로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId)
                .set(PROJECT.TITLE, "진행 중인 프로젝트")
                .set(PROJECT.DESCRIPTION, "현재 진행 중인 프로젝트")
                .set(PROJECT.CONTENT, "진행 중인 프로젝트 상세 내용")
                .set(PROJECT.MEMBER_ID, 1L)
                .set(PROJECT.CATEGORY, "웹 개발")
                .set(PROJECT.SUBCATEGORY, "풀스택")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.minusDays(1)) // 1일 전 시작
                .set(PROJECT.ENDED_AT, now.plusDays(30))   // 30일 후 종료
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();
        
        // When & Then - 진행 중인 프로젝트 조회
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", String.valueOf(projectId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.status").value("INPROGRESS")); // 진행 중 상태 확인
    }

    @Test
    @Order(201)
    @DisplayName("✅ 프로젝트 목록 조회 - Status 값 검증")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_return_correct_project_status_in_list() throws Exception {
        // Given
        setupTestData();
        
        // 프로젝트를 현재 진행 중인 상태로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, 1L)
                .set(PROJECT.TITLE, "진행 중인 프로젝트")
                .set(PROJECT.DESCRIPTION, "현재 진행 중인 프로젝트")
                .set(PROJECT.CONTENT, "진행 중인 프로젝트 상세 내용")
                .set(PROJECT.MEMBER_ID, 1L)
                .set(PROJECT.CATEGORY, "웹 개발")
                .set(PROJECT.SUBCATEGORY, "풀스택")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.minusDays(1)) // 1일 전 시작
                .set(PROJECT.ENDED_AT, now.plusDays(30))   // 30일 후 종료
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();
        
        // When & Then - 프로젝트 목록 조회
        mockMvc.perform(get("/api/v1/project")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].status").value("INPROGRESS")); // 진행 중 상태 확인
    }

    @Test
    @Order(202)
    @DisplayName("✅ 완료된 프로젝트 - Status 값 검증")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_return_completed_status_for_ended_project() throws Exception {
        // Given
        Long projectId = 1L;
        setupTestData();
        createTestProjectInDatabase();
        
        // 프로젝트를 완료된 상태로 설정
        dsl.update(PROJECT)
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().minusDays(10))
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().minusDays(1))
                .where(PROJECT.ID.eq(projectId))
                .execute();
        
        // When & Then - 완료된 프로젝트 조회
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", String.valueOf(projectId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED")); // 완료 상태 확인
    }

    @Test
    @Order(203)
    @DisplayName("✅ 준비 중인 프로젝트 - Status 값 검증")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void should_return_ready_status_for_future_project() throws Exception {
        // Given
        Long projectId = 1L;
        setupTestData();
        
        // 프로젝트를 처음부터 미래 날짜로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId)
                .set(PROJECT.TITLE, "미래 프로젝트")
                .set(PROJECT.DESCRIPTION, "미래에 시작될 프로젝트")
                .set(PROJECT.CONTENT, "미래 프로젝트 상세 내용")
                .set(PROJECT.MEMBER_ID, 1L)
                .set(PROJECT.CATEGORY, "웹 개발")
                .set(PROJECT.SUBCATEGORY, "풀스택")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(10)) // 10일 후 시작
                .set(PROJECT.ENDED_AT, now.plusDays(40))   // 40일 후 종료
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();
        
        // When & Then - 준비 중인 프로젝트 조회
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", String.valueOf(projectId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.status").value("READY")); // 준비 중 상태 확인
    }

    @Test
    @Order(300)
    @DisplayName("✅ 프로젝트 상세 조회 - 첨부파일이 존재하면 배열에 채워진다")
    void should_return_attachments_in_project_detail_when_exist() throws Exception {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, TEST_PROJECT_TITLE)
                .set(PROJECT.DESCRIPTION, TEST_PROJECT_DESCRIPTION)
                .set(PROJECT.CONTENT, TEST_PROJECT_CONTENT)
                .set(PROJECT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT.CATEGORY, TEST_PROJECT_CATEGORY)
                .set(PROJECT.SUBCATEGORY, TEST_PROJECT_SUBCATEGORY)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.plusDays(1))
                .set(PROJECT.ENDED_AT, now.plusDays(30))
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();

        dsl.insertInto(PROJECT_ATTACHED)
                .set(PROJECT_ATTACHED.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_ATTACHED.NAME, "spec.pdf")
                .set(PROJECT_ATTACHED.TYPE, "application/pdf")
                .set(PROJECT_ATTACHED.SIZE, "12345")
                .set(PROJECT_ATTACHED.ATTACHED_URL, "https://s3.example.com/spec.pdf")
                .set(PROJECT_ATTACHED.CREATED_AT, now)
                .set(PROJECT_ATTACHED.UPDATED_AT, now)
                .execute();

        // When & Then
        mockMvc.perform(get("/api/v1/project/detail")
                        .param("projectId", TEST_PROJECT_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value("https://s3.example.com/spec.pdf"));
    }
}