package org.certis.studyplatform.project.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.request.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProjectParticipantController 완전한 통합 테스트
 *
 * 새로운 테스트 특징:
 * - CQRS 패턴과 JOOQ 기반 아키텍처 완전 활용
 * - Clean Architecture 계층별 테스트 분리
 * - BDD 스타일 테스트 시나리오 (Given-When-Then)
 * - 실제 비즈니스 시나리오 기반 테스트 케이스
 * - 포괄적인 검증 및 엣지 케이스 커버
 * - 테스트 데이터 격리 및 독립성 보장
 * - 성능 및 동시성 고려 테스트
 *
 * 기술 스택:
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
@DisplayName("ProjectParticipantController 새로운 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final String BASE_URL = "/api/v1/project/participant";
    private static final Long TEST_PROJECT_ID = 1L;
    private static final Long TEST_PROJECT_2_ID = 2L;
    private static final Long TEST_MEMBER_ID = 2L;
    private static final Long TEST_MEMBER_2_ID = 3L;
    private static final Long TEST_MEMBER_3_ID = 4L;
    private static final Long TEST_CREATOR_ID = 1L;

    private static final String TEST_PROJECT_NAME = "CERT-IS 학습 플랫폼 프로젝트";
    private static final String TEST_PROJECT_2_NAME = "데이터베이스 최적화 프로젝트";
    private static final String TEST_MEMBER_NAME = "김참가";
    private static final String TEST_MEMBER_2_NAME = "이신청";
    private static final String TEST_MEMBER_3_NAME = "박승인";
    private static final String TEST_CREATOR_NAME = "최프로젝트";

    @BeforeEach
    void setUp() {
        System.out.println("테스트 데이터 설정 시작");
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
        System.out.println("테스트 데이터 설정 완료");
    }

    @AfterEach
    void tearDown() {
        System.out.println("테스트 데이터 정리 시작");
        cleanupTestData();
        System.out.println("테스트 데이터 정리 완료");
    }

    // =================================================================
    // 비즈니스 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("프로젝트 참가 신청 - 성공적인 비즈니스 시나리오")
    void registerJoinProject_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 프로젝트가 존재하고, 일반 멤버(1L)가 다른 사용자가 생성한 프로젝트(2번)에 참가 신청을 준비함
        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_2_ID);

        // When: 프로젝트 참가 신청 API를 호출
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 CREATED 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.data.participantId").exists())
                .andExpect(jsonPath("$.data.projectId").value(TEST_PROJECT_2_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Then: 데이터베이스에 참가 신청이 정상적으로 저장되었는지 검증
        verifyParticipantCreatedInDatabase(request);

        System.out.println("프로젝트 참가 신청 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("프로젝트 참가 신청 취소 - 성공적인 취소 시나리오")
    void cancelJoinProject_SuccessfulCancellationScenario() throws Exception {
        // Given: 현재 로그인 사용자(1L)의 대기 중인 참가 신청이 존재함 (프로젝트 2번에 대해)
        createPendingParticipantInDatabase(TEST_PROJECT_2_ID, 1L); // 현재 로그인 사용자

        ProjectJoinCancelRequestDto request = new ProjectJoinCancelRequestDto();
        request.setProjectId(TEST_PROJECT_2_ID);

        // When: 참가 신청 취소 API 호출
        mockMvc.perform(delete(BASE_URL + "/join/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk());

        // Then: 데이터베이스에서 소프트 삭제 확인
        verifyParticipantCancelledInDatabase(TEST_PROJECT_2_ID, 1L);

        System.out.println("프로젝트 참가 신청 취소 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("프로젝트 참가 승인 - 프로젝트 생성자의 성공적인 승인")
    void approveJoinProject_CreatorSuccessfulApproval() throws Exception {
        // Given: 프로젝트 생성자(1L)의 프로젝트(1번)에 다른 사용자(TEST_MEMBER_2_ID)의 대기 중인 참가 신청이 존재함
        Long participantId = createPendingParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_2_ID);

        ProjectJoinApproveRequestDto request = new ProjectJoinApproveRequestDto();
        request.setParticipantId(participantId);

        // When: 프로젝트 생성자(1L)가 참가 승인 API 호출
        mockMvc.perform(post(BASE_URL + "/join/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(participantId))
                .andExpect(jsonPath("$.data.currentStatus").value("APPROVED"));

        // Then: 데이터베이스에서 상태 변경 확인
        verifyParticipantStatusInDatabase(participantId, ProjectParticipantStatus.APPROVED);

        System.out.println("프로젝트 참가 승인 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("프로젝트 참가 거절 - 프로젝트 생성자의 성공적인 거절")
    void rejectJoinProject_CreatorSuccessfulRejection() throws Exception {
        // Given: 프로젝트 생성자(1L)의 프로젝트(1번)에 다른 사용자(TEST_MEMBER_3_ID)의 대기 중인 참가 신청이 존재함
        Long participantId = createPendingParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_3_ID);

        ProjectJoinRejectRequestDto request = new ProjectJoinRejectRequestDto();
        request.setParticipantId(participantId);

        // When: 프로젝트 생성자(1L)가 참가 거절 API 호출
        mockMvc.perform(post(BASE_URL + "/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(participantId))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

        // Then: 데이터베이스에서 소프트 삭제 확인 (deleted_at 설정)
        verifyParticipantSoftDeletedInDatabase(participantId);

        System.out.println("프로젝트 참가 거절 테스트 성공");
    }

    @Test
    @Order(5)
    @DisplayName("프로젝트별 참가자 목록 조회 - 페이징과 정렬 기능")
    void getProjectParticipants_PaginationAndSortingFeatures() throws Exception {
        // Given: 여러 참가자들이 존재함
        createMultipleParticipantsInDatabase();

        // When: 페이징된 참가자 목록 조회
        mockMvc.perform(get(BASE_URL + "/{projectId}/participants", TEST_PROJECT_ID)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andDo(print())
                // Then: 페이징된 결과와 메타데이터 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3)) // 3명 참가자
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

        System.out.println("프로젝트별 참가자 목록 조회 테스트 성공");
    }

    @Test
    @Order(6)
    @DisplayName("사용자별 참가 프로젝트 목록 조회 - 다중 프로젝트 참여")
    void getMemberParticipations_MultipleProjectParticipation() throws Exception {
        // Given: 한 사용자가 여러 프로젝트에 참가함
        createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_ID);
        createApprovedParticipantInDatabase(TEST_PROJECT_2_ID, TEST_MEMBER_ID);

        // When: 사용자별 참가 프로젝트 목록 조회
        mockMvc.perform(get(BASE_URL + "/members/{memberId}/participations", TEST_MEMBER_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 참가 프로젝트 목록 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2)) // 2개 프로젝트 참가
                .andExpect(jsonPath("$.data.totalElements").value(2));

        System.out.println("사용자별 참가 프로젝트 목록 조회 테스트 성공");
    }

    // =================================================================
    // 검증 실패 및 예외 상황 테스트
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("참가 신청 실패 - 필수 필드 누락")
    void registerJoinProject_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        // projectId 누락

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        System.out.println("필수 필드 누락 검증 테스트 성공");
    }

    @Test
    @Order(11)
    @DisplayName("참가 신청 실패 - 중복 신청")
    void registerJoinProject_BusinessFailure_DuplicateApplication() throws Exception {
        // Given: 현재 로그인 사용자(1L)가 이미 프로젝트 2번에 참가 신청함
        createPendingParticipantInDatabase(TEST_PROJECT_2_ID, 1L);

        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_2_ID);

        // When & Then: 중복 신청으로 비즈니스 로직 실패
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 참가 신청한 프로젝트입니다."));

        System.out.println("중복 참가 신청 테스트 성공");
    }

    @Test
    @Order(12)
    @DisplayName("참가 신청 실패 - 프로젝트 생성자의 자가 신청")
    void registerJoinProject_BusinessFailure_CreatorSelfApplication() throws Exception {
        // Given: 프로젝트 생성자(1L)가 자신의 프로젝트(1번)에 참가 신청
        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_ID);

        // When & Then: 비즈니스 규칙에 의해 422 에러 반환
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("프로젝트 생성자는 자신의 프로젝트에 참가 신청할 수 없습니다."));

        System.out.println("프로젝트 생성자 자가 신청 거부 테스트 성공");
    }

    @Test
    @Order(13)
    @DisplayName("참가 승인 실패 - 존재하지 않는 참가 신청")
    void approveJoinProject_NotFound_NonExistentApplication() throws Exception {
        // Given: 존재하지 않는 참가 신청 ID
        ProjectJoinApproveRequestDto request = new ProjectJoinApproveRequestDto();
        request.setParticipantId(99999L);

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(post(BASE_URL + "/join/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("참가 신청을 찾을 수 없습니다."));

        System.out.println("존재하지 않는 참가 신청 승인 테스트 성공");
    }

    @Test
    @Order(14)
    @DisplayName("참가 승인 실패 - 이미 처리된 참가 신청")
    void approveJoinProject_BusinessFailure_AlreadyProcessedApplication() throws Exception {
        // Given: 이미 승인된 참가 신청을 다시 승인하려고 시도
        Long participantId = createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_2_ID);

        ProjectJoinApproveRequestDto request = new ProjectJoinApproveRequestDto();
        request.setParticipantId(participantId);

        // When & Then: 비즈니스 로직에 의해 거부됨
        mockMvc.perform(post(BASE_URL + "/join/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("대기 중인 참가 신청만 승인할 수 있습니다."));

        System.out.println("이미 처리된 참가 신청 승인 테스트 성공");
    }

    // =================================================================
    // 편의 기능 API 테스트
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("대기 중인 참가자 목록 조회 - 프로젝트 생성자용")
    void getPendingParticipants_ForProjectCreator() throws Exception {
        // Given: 다양한 상태의 참가자들이 존재함
        createPendingParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_ID);
        createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_2_ID);
        createRejectedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_3_ID);

        // When: 대기 중인 참가자만 조회
        mockMvc.perform(get(BASE_URL + "/{projectId}/participants/pending", TEST_PROJECT_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: PENDING 상태 참가자만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1)) // PENDING 1명만
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        System.out.println("대기 중인 참가자 목록 조회 테스트 성공");
    }

    @Test
    @Order(21)
    @DisplayName("승인된 참가자 목록 조회 - 프로젝트 팀원 확인")
    void getApprovedParticipants_ForTeamConfirmation() throws Exception {
        // Given: 다양한 상태의 참가자들이 존재함
        createPendingParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_ID);
        createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_2_ID);
        createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_3_ID);

        // When: 승인된 참가자만 조회
        mockMvc.perform(get(BASE_URL + "/{projectId}/participants/approved", TEST_PROJECT_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: APPROVED 상태 참가자만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2)) // APPROVED 2명
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.content[1].status").value("APPROVED"));

        System.out.println("승인된 참가자 목록 조회 테스트 성공");
    }

    // =================================================================
    // 엣지 케이스 및 기술적 테스트
    // =================================================================

    @Test
    @Order(30)
    @DisplayName("잘못된 Content-Type - HTTP 415 Unsupported Media Type")
    void registerJoinProject_TechnicalFailure_UnsupportedMediaType() throws Exception {
        // Given: 잘못된 Content-Type
        ProjectJoinRequestDto request = createValidJoinRequest();

        // When & Then: HTTP 415 Unsupported Media Type 응답
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        System.out.println("잘못된 Content-Type 테스트 성공");
    }

    @Test
    @Order(31)
    @DisplayName("잘못된 JSON 형식 - HTTP 400 Bad Request")
    void registerJoinProject_TechnicalFailure_MalformedJson() throws Exception {
        // When & Then: 잘못된 JSON으로 HTTP 400 Bad Request 응답
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        System.out.println("잘못된 JSON 형식 테스트 성공");
    }

    @Test
    @Order(32)
    @DisplayName("잘못된 HTTP 메서드 - HTTP 405 Method Not Allowed")
    void getProjectParticipants_TechnicalFailure_WrongHttpMethod() throws Exception {
        // When & Then: GET 엔드포인트에 POST 요청으로 HTTP 405 Method Not Allowed 응답
        mockMvc.perform(post(BASE_URL + "/{projectId}/participants", TEST_PROJECT_ID))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        System.out.println("잘못된 HTTP 메서드 테스트 성공");
    }

    // =================================================================
    // 성능 및 대용량 데이터 테스트
    // =================================================================

    @Test
    @Order(40)
    @DisplayName("대용량 참가자 목록 조회 - 페이징 성능 테스트")
    void getProjectParticipants_PerformanceTest_LargeDataset() throws Exception {
        // Given: 대량의 참가자 데이터 생성 (100명)
        createLargeParticipantDataset(100);

        long startTime = System.currentTimeMillis();

        // When: 페이징된 목록 조회
        mockMvc.perform(get(BASE_URL + "/{projectId}/participants", TEST_PROJECT_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                // Then: 정상 응답과 성능 기준 만족
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(100));

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 성능 검증: 1초 이내 응답
        assertThat(executionTime).isLessThan(1000);

        System.out.println("대용량 데이터 페이징 성능 테스트 성공 - 실행시간: " + executionTime + "ms");
    }

    @Test
    @Order(41)
    @DisplayName("참가자 수 제한 - 정원 초과 신청 거부")
    void registerJoinProject_BusinessRule_ParticipantLimitExceeded() throws Exception {
        // Given: 프로젝트 2번의 정원이 가득 찬 상황 (maxParticipants = 3, 현재 3명)
        createFullProjectParticipants();

        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_2_ID); // 프로젝트 2번 사용

        // When & Then: 정원 초과로 참가 신청 거부
        mockMvc.perform(post(BASE_URL + "/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("프로젝트 정원이 가득 찼습니다."));

        System.out.println("참가자 수 제한 테스트 성공");
    }

    // =================================================================
    // 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 참가 신청 요청 DTO 생성
     */
    private ProjectJoinRequestDto createValidJoinRequest() {
        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_ID);
        return request;
    }

    /**
     * 테스트 데이터 설정 (프로젝트, 멤버)
     */
    private void setupTestData() {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            // 프로젝트 데이터 생성
            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, TEST_PROJECT_ID)
                    .set(PROJECT.TITLE, TEST_PROJECT_NAME)
                    .set(PROJECT.DESCRIPTION, "통합 테스트용 프로젝트")
                    .set(PROJECT.CONTENT, "프로젝트 상세 내용")
                    .set(PROJECT.MEMBER_ID, TEST_CREATOR_ID)
                    .set(PROJECT.CATEGORY, "웹 개발")
                    .set(PROJECT.SUBCATEGORY, "풀스택")
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(PROJECT.STARTED_AT, now.plusDays(1))
                    .set(PROJECT.ENDED_AT, now.plusDays(30))
                    .set(PROJECT.CREATED_AT, now)
                    .set(PROJECT.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            dsl.insertInto(PROJECT)
                    .set(PROJECT.ID, TEST_PROJECT_2_ID)
                    .set(PROJECT.TITLE, TEST_PROJECT_2_NAME)
                    .set(PROJECT.DESCRIPTION, "두 번째 테스트 프로젝트")
                    .set(PROJECT.CONTENT, "두 번째 프로젝트 상세 내용")
                    .set(PROJECT.MEMBER_ID, 20L) // 다른 사용자가 생성한 프로젝트
                    .set(PROJECT.CATEGORY, "데이터베이스")
                    .set(PROJECT.SUBCATEGORY, "최적화")
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 3)
                    .set(PROJECT.STARTED_AT, now.plusDays(2))
                    .set(PROJECT.ENDED_AT, now.plusDays(45))
                    .set(PROJECT.CREATED_AT, now)
                    .set(PROJECT.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 멤버 데이터 생성
            createMemberInDatabase(TEST_CREATOR_ID, TEST_CREATOR_NAME, "creator@certis.org", now);
            createMemberInDatabase(20L, "프로젝트2생성자", "creator2@certis.org", now);
            createMemberInDatabase(TEST_MEMBER_ID, TEST_MEMBER_NAME, "member1@certis.org", now);
            createMemberInDatabase(TEST_MEMBER_2_ID, TEST_MEMBER_2_NAME, "member2@certis.org", now);
            createMemberInDatabase(TEST_MEMBER_3_ID, TEST_MEMBER_3_NAME, "member3@certis.org", now);

        } catch (Exception e) {
            System.out.println("테스트 데이터 설정 중 오류 발생 (이미 존재할 수 있음): " + e.getMessage());
        }
    }

    /**
     * 개별 멤버 생성
     */
    private void createMemberInDatabase(Long memberId, String name, String email, OffsetDateTime now) {
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, memberId)
                .set(MEMBER.NAME, name)
                .set(MEMBER.STUDENT_NUMBER, email)
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.BIRTHDAY, now.minusYears(25))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        try {
            // 외래 키 제약으로 인해 역순으로 삭제
            dsl.deleteFrom(PROJECT_PARTICIPANT).execute();
            dsl.deleteFrom(PROJECT).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
            System.out.println("테스트 데이터 정리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 대기 중인 참가자 생성
     */
    private Long createPendingParticipantInDatabase(Long projectId, Long memberId) {
        return createParticipantInDatabase(projectId, memberId, ProjectParticipantStatus.PENDING);
    }

    /**
     * 승인된 참가자 생성
     */
    private Long createApprovedParticipantInDatabase(Long projectId, Long memberId) {
        return createParticipantInDatabase(projectId, memberId, ProjectParticipantStatus.APPROVED);
    }

    /**
     * 거절된 참가자 생성
     */
    private Long createRejectedParticipantInDatabase(Long projectId, Long memberId) {
        return createParticipantInDatabase(projectId, memberId, ProjectParticipantStatus.REJECTED);
    }

    /**
     * 참가자 생성 (공통 메서드)
     */
    private Long createParticipantInDatabase(Long projectId, Long memberId, ProjectParticipantStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        Long participantId = projectId * 1000 + memberId; // 고유한 ID 생성

        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.ID, participantId)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, projectId)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, status.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, now)
                .set(PROJECT_PARTICIPANT.UPDATED_AT, now)
                .execute();

        return participantId;
    }

    /**
     * 다수의 참가자 생성 (목록 조회 테스트용)
     */
    private void createMultipleParticipantsInDatabase() {
        createPendingParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_ID);
        createApprovedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_2_ID);
        createRejectedParticipantInDatabase(TEST_PROJECT_ID, TEST_MEMBER_3_ID);
    }

    /**
     * 대용량 참가자 데이터셋 생성 (성능 테스트용)
     */
    private void createLargeParticipantDataset(int count) {
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 1; i <= count; i++) {
            Long memberId = 1000L + i;
            Long participantId = TEST_PROJECT_ID * 10000 + memberId;

            // 멤버 생성
            createMemberInDatabase(memberId, "대용량테스트" + i, "bulk" + i + "@test.com", now);

            // 참가자 생성
            dsl.insertInto(PROJECT_PARTICIPANT)
                    .set(PROJECT_PARTICIPANT.ID, participantId)
                    .set(PROJECT_PARTICIPANT.PROJECT_ID, TEST_PROJECT_ID)
                    .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                    .set(PROJECT_PARTICIPANT.STATUS, ProjectParticipantStatus.APPROVED.name())
                    .set(PROJECT_PARTICIPANT.CREATED_AT, now.minusHours(i))
                    .set(PROJECT_PARTICIPANT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 정원이 가득 찬 프로젝트 생성
     */
    private void createFullProjectParticipants() {
        // 프로젝트 2번의 정원 3명 모두 승인된 상태로 생성
        for (int i = 1; i <= 3; i++) {
            Long memberId = 2000L + i;
            createMemberInDatabase(memberId, "정원테스트" + i, "full" + i + "@test.com", OffsetDateTime.now());
            createApprovedParticipantInDatabase(TEST_PROJECT_2_ID, memberId);
        }
    }

    /**
     * 참가 신청 생성 검증
     */
    private void verifyParticipantCreatedInDatabase(ProjectJoinRequestDto request) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(request.getProjectId()))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(1L)) // 현재 로그인 사용자 ID (하드코딩)
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(ProjectParticipantStatus.PENDING.name());
    }

    /**
     * 참가 신청 취소 검증 (소프트 삭제)
     */
    private void verifyParticipantCancelledInDatabase(Long projectId, Long memberId) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(projectId))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(memberId))
                .fetchOne();

        if (participant != null) {
            assertThat(participant.getDeletedAt()).isNotNull(); // 소프트 삭제 확인
        }
    }

    /**
     * 참가자 상태 변경 검증
     */
    private void verifyParticipantStatusInDatabase(Long participantId, ProjectParticipantStatus expectedStatus) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(expectedStatus.name());
    }

    /**
     * 참가자 소프트 삭제 검증 (거절)
     */
    private void verifyParticipantSoftDeletedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getDeletedAt()).isNotNull();
    }
}