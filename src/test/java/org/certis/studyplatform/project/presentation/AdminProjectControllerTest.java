package org.certis.studyplatform.project.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.request.AdminProjectParticipantApprovalRequestDto;
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

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminProjectController 완전한 통합 테스트
 *
 * 🎯 테스트 특징:
 * - 관리자 권한 기반 프로젝트 참가 승인/거절 API 테스트
 * - CQRS 패턴과 JOOQ 기반 아키텍처 완전 활용
 * - Clean Architecture 계층별 테스트 분리
 * - 권한 기반 접근 제어 테스트
 * - 비즈니스 시나리오 기반 테스트 케이스
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
@DisplayName("🚀 AdminProjectController 완전한 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    // 테스트 상수
    private static final String BASE_URL = "/api/v1/admin/project";
    private static final Long TEST_ADMIN_ID = 3L; // MockCurrentUserProvider에서 "staff"는 ID 3L로 매핑됨
    private static final Long TEST_MEMBER_ID = 2L;
    private static final Long TEST_PROJECT_ID = 1L;
    private static final Long TEST_PARTICIPANT_ID = 1L;

    private static final String TEST_ADMIN_NAME = "관리자";
    private static final String TEST_MEMBER_NAME = "테스트회원";
    private static final String TEST_PROJECT_TITLE = "Spring Boot 프로젝트";

    @BeforeEach
    void setUp() {
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    // =================================================================
    // 🎯 성공 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("✅ 관리자가 프로젝트 참가 신청을 성공적으로 승인한다")
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void admin_should_approve_project_participant_successfully() throws Exception {
        // Given: 승인할 참가 신청이 존재하는 상태
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setParticipantId(TEST_PARTICIPANT_ID);
        request.setReason("자격 요건 충족");

        // When: 관리자가 참가 신청을 승인
        mockMvc.perform(post(BASE_URL + "/participant/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 관리자에 의해 성공적으로 승인되었습니다"))
                .andExpect(jsonPath("$.data.participantId").value(TEST_PARTICIPANT_ID))
                .andExpect(jsonPath("$.data.projectId").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.projectTitle").value(TEST_PROJECT_TITLE))
                .andExpect(jsonPath("$.data.memberId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.memberName").value(TEST_MEMBER_NAME))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reason").value("자격 요건 충족"))
                .andExpect(jsonPath("$.data.adminId").value(TEST_ADMIN_ID))
                .andExpect(jsonPath("$.data.processedAt").exists());

        // Then: 데이터베이스에서 상태가 APPROVED로 변경되었는지 확인
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(TEST_PARTICIPANT_ID))
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(ProjectParticipantStatus.APPROVED.name());
    }

    @Test
    @Order(2)
    @DisplayName("✅ 관리자가 프로젝트 참가 신청을 성공적으로 거절한다")
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void admin_should_reject_project_participant_successfully() throws Exception {
        // Given: 거절할 참가 신청이 존재하는 상태
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setParticipantId(TEST_PARTICIPANT_ID);
        request.setReason("자격 요건 미충족");

        // When: 관리자가 참가 신청을 거절
        mockMvc.perform(post(BASE_URL + "/participant/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 관리자에 의해 성공적으로 거절되었습니다"))
                .andExpect(jsonPath("$.data.participantId").value(TEST_PARTICIPANT_ID))
                .andExpect(jsonPath("$.data.projectId").value(TEST_PROJECT_ID))
                .andExpect(jsonPath("$.data.projectTitle").value(TEST_PROJECT_TITLE))
                .andExpect(jsonPath("$.data.memberId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.memberName").value(TEST_MEMBER_NAME))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reason").value("자격 요건 미충족"))
                .andExpect(jsonPath("$.data.adminId").value(TEST_ADMIN_ID))
                .andExpect(jsonPath("$.data.processedAt").exists());

        // Then: 데이터베이스에서 상태가 REJECTED로 변경되었는지 확인
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(TEST_PARTICIPANT_ID))
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(ProjectParticipantStatus.REJECTED.name());
    }

    // =================================================================
    // 🚫 권한 기반 접근 제어 테스트
    // =================================================================

    @Test
    @Order(3)
    @DisplayName("❌ 일반 사용자는 프로젝트 참가 승인 API에 접근할 수 없다")
    @WithMockUser(username = "user", roles = {"PLAYER"})
    void regular_user_should_not_access_approve_api() throws Exception {
        // Given: 일반 사용자 권한
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setParticipantId(TEST_PARTICIPANT_ID);

        // When & Then: 접근 거부
        mockMvc.perform(post(BASE_URL + "/participant/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("❌ 일반 사용자는 프로젝트 참가 거절 API에 접근할 수 없다")
    @WithMockUser(username = "user", roles = {"PLAYER"})
    void regular_user_should_not_access_reject_api() throws Exception {
        // Given: 일반 사용자 권한
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setParticipantId(TEST_PARTICIPANT_ID);

        // When & Then: 접근 거부
        mockMvc.perform(post(BASE_URL + "/participant/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // =================================================================
    // 🔍 유효성 검증 테스트
    // =================================================================

    @Test
    @Order(5)
    @DisplayName("❌ 참가자 ID가 없으면 400 에러가 발생한다")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return_400_when_participant_id_is_null() throws Exception {
        // Given: 참가자 ID가 없는 요청
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setReason("테스트");

        // When & Then: 400 에러
        mockMvc.perform(post(BASE_URL + "/participant/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("❌ 존재하지 않는 참가자 ID로 요청하면 400 에러가 발생한다")
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void should_return_400_when_participant_not_found() throws Exception {
        // Given: 존재하지 않는 참가자 ID
        AdminProjectParticipantApprovalRequestDto request = new AdminProjectParticipantApprovalRequestDto();
        request.setParticipantId(999L);
        request.setReason("테스트");

        // When & Then: 404 에러
        mockMvc.perform(post(BASE_URL + "/participant/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // 🛠 테스트 데이터 설정 및 정리
    // =================================================================

    private void setupTestData() {
        // 관리자 계정 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_ADMIN_ID)
                .set(MEMBER.NAME, TEST_ADMIN_NAME)
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240001")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, java.time.OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, java.time.OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();

        // 테스트 회원 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, TEST_MEMBER_NAME)
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.STUDENT_NUMBER, "20240002")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, java.time.OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, java.time.OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();

        // 테스트 프로젝트 생성
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, TEST_PROJECT_TITLE)
                .set(PROJECT.DESCRIPTION, "Spring Boot 기반 웹 애플리케이션 개발")
                .set(PROJECT.CONTENT, "Spring Boot를 활용한 REST API 개발")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "백엔드")
                .set(PROJECT.STARTED_AT, java.time.OffsetDateTime.now().plusDays(1))
                .set(PROJECT.ENDED_AT, java.time.OffsetDateTime.now().plusDays(60))
                .set(PROJECT.MEMBER_ID, TEST_ADMIN_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                // .set(PROJECT.CURRENT_PARTICIPANTS, 0) // CURRENT_PARTICIPANTS 필드가 없음
                // .set(PROJECT.STATUS, "RECRUITING") // STATUS 필드가 없음
                // .set(PROJECT.SEMESTER, "2024-1") // SEMESTER 필드가 없음
                .set(PROJECT.GITHUB_URL, "https://github.com/test/project")
                .set(PROJECT.EXTERNAL_URL, "{\"title\":\"프로젝트 사이트\",\"url\":\"https://project.example.com\"}")
                // .set(PROJECT.DEMO_URL, "https://demo.example.com/project") // DEMO_URL 필드가 없음
                .set(PROJECT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();

        // 테스트 참가 신청 생성
        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.ID, TEST_PARTICIPANT_ID)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, TEST_PROJECT_ID)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, TEST_MEMBER_ID)
                .set(PROJECT_PARTICIPANT.STATUS, ProjectParticipantStatus.PENDING.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, java.time.OffsetDateTime.now())
                .set(PROJECT_PARTICIPANT.UPDATED_AT, java.time.OffsetDateTime.now())
                .execute();
    }

    private void cleanupTestData() {
        // 테스트 데이터 정리
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
    }
}
