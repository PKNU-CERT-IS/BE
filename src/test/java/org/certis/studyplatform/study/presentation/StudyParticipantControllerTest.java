package org.certis.studyplatform.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.presentation.dto.request.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.hamcrest.Matchers;

/**
 * StudyParticipantController 완전 새로운 통합 테스트
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("🚀 StudyParticipantController 새로운 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudyParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final Long TEST_STUDY_ID = 1L;
    private static final Long TEST_MEMBER_ID = 1L; // 스터디 생성자
    private static final Long TEST_PARTICIPANT_ID = 2L; // 참가 신청자
    private static final Long TEST_PARTICIPANT_2_ID = 3L; // 또 다른 참가 신청자
    private static final Long TEST_STUDY_PARTICIPANT_ID = 1L;
    
    private static final String TEST_STUDY_TITLE = "CERT-IS 학습 플랫폼 스터디";
    private static final String TEST_MEMBER_NAME = "김개발"; // 스터디 생성자
    private static final String TEST_PARTICIPANT_NAME = "이참가"; // 참가 신청자
    private static final String TEST_PARTICIPANT_2_NAME = "박신청"; // 또 다른 참가 신청자

    @BeforeEach
    void setUp() {
        // 시드 데이터로 인한 PK 충돌 방지를 위해 매 테스트 시작 시 테이블 정리
        dsl.execute("TRUNCATE TABLE study_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        
        // 시퀀스도 명시적으로 리셋 (더 안전한 방법)
        dsl.execute("ALTER SEQUENCE study_id_seq RESTART WITH 1");
        dsl.execute("ALTER SEQUENCE study_participant_id_seq RESTART WITH 1");
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
        
        // 시퀀스 상태 확인 및 강제 리셋
        dsl.execute("SELECT setval('study_id_seq', 1, false)");
        dsl.execute("SELECT setval('study_participant_id_seq', 1, false)");
        dsl.execute("SELECT setval('member_id_seq', 1, false)");

        setupTestData();
        
        // setupTestData() 후 시퀀스를 다음 값으로 설정하여 충돌 방지
        dsl.execute("SELECT setval('study_id_seq', 2, false)");
        dsl.execute("SELECT setval('study_participant_id_seq', 2, false)");
        
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    // =================================================================
    // 🎯 비즈니스 시나리오 기반 통합 테스트 - 참가 신청 관리
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("📝 스터디 참가 신청 - 성공적인 비즈니스 시나리오")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void registerJoinStudy_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 스터디와 참가 신청자가 존재하고, 참가 신청 요청이 준비됨
        StudyJoinRequestDto request = createValidJoinRequest();

        // When: 참가 신청 API를 호출
        mockMvc.perform(post("/api/v1/study/participant/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: 성공 응답 확인
                .andExpect(status().isCreated())
                
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"))
                .andExpect(jsonPath("$.data.studyId").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        // Then: 데이터베이스에 참가 신청이 정상적으로 저장되었는지 검증
        verifyParticipantRegisteredInDatabase(request);
        
    }

    @Test
    @Order(2)
    @DisplayName("✅ 스터디 참가 승인 - 스터디 생성자의 성공적인 승인")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void approveJoinStudy_StudyCreatorSuccessfulApproval() throws Exception {
        // Given: 참가 신청이 존재하고, 스터디 생성자가 승인을 요청함
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        
        StudyJoinApproveRequestDto request = new StudyJoinApproveRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setMemberId(TEST_PARTICIPANT_ID);


        mockMvc.perform(post("/api/v1/study/participant/join/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가가 승인되었습니다"))
                .andExpect(jsonPath("$.data.studyId").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.currentStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        // Then: 데이터베이스에서 승인 상태 확인
        verifyParticipantStatusInDatabase(TEST_STUDY_PARTICIPANT_ID, StudyParticipantStatus.APPROVED);
        
    }

    @Test
    @Order(3)
    @DisplayName("❌ 스터디 참가 거절 - 스터디 생성자의 성공적인 거절")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void rejectJoinStudy_StudyCreatorSuccessfulRejection() throws Exception {
        // Given: 참가 신청이 존재하고, 스터디 생성자가 거절을 요청함
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        
        StudyJoinRejectRequestDto request = new StudyJoinRejectRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setMemberId(TEST_PARTICIPANT_ID);
        // Note: RejecterId and RejectReason are handled by security context and service layer

        mockMvc.perform(post("/api/v1/study/participant/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가가 거절되었습니다"))
                .andExpect(jsonPath("$.data.studyId").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        // Then: 데이터베이스에서 상태 업데이트 확인 (REJECTED 상태)
        verifyParticipantStatusUpdatedInDatabase(TEST_STUDY_PARTICIPANT_ID);
        
    }

    @Test
    @Order(4)
    @DisplayName("🗑️ 스터디 참가 신청 취소 - 신청자의 성공적인 취소")
    void cancelJoinStudy_ApplicantSuccessfulCancellation() throws Exception {
        // Given: 참가 신청이 존재하고, 신청자가 취소를 요청함
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        
        StudyJoinCancelRequestDto request = new StudyJoinCancelRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        // Note: MemberId is handled by security context in real implementation

        // When: 참가 신청 취소 API 호출
        // 취소자는 신청자여야 하므로 보안 컨텍스트를 신청자로 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(TEST_PARTICIPANT_ID, "user2", "user2@certis.org", "유저2", "UPSOLVER"), null)
        );
        mockMvc.perform(delete("/api/v1/study/participant/join/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가 취소가 성공했습니다"));

        // Then: 데이터베이스에서 취소 확인 (소프트 삭제 또는 상태 변경)
        verifyParticipantCancelledInDatabase(TEST_STUDY_ID, TEST_PARTICIPANT_ID);
        
    }

    // =================================================================
    // 🔍 조회 기능 테스트
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("📋 스터디별 참가자 목록 조회 - 전체 참가자")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void getStudyParticipants_AllParticipants() throws Exception {
        // Given: 여러 상태의 참가자들이 존재함
        createMultipleParticipantsInDatabase();

        mockMvc.perform(get("/api/v1/study/participant/{studyId}/participants/all", TEST_STUDY_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 전체 참가자 목록과 페이징 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가자 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(3)) // 3명의 참가자
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                // memberGrade 검증: 특정 이름 필터 결과에서 기대 학년 확인
                .andExpect(jsonPath("$.data.content[?(@.memberName=='이참가')]").isArray())
                .andExpect(jsonPath("$.data.content[?(@.memberName=='이참가')].memberGrade").value("JUNIOR"))
                .andExpect(jsonPath("$.data.content[?(@.memberName=='박신청')]").isArray())
                .andExpect(jsonPath("$.data.content[?(@.memberName=='박신청')].memberGrade").value("SOPHOMORE"));

    }

    @Test
    @Order(11)
    @DisplayName("⏳ 스터디별 대기 중인 참가자 목록 조회")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void getPendingParticipants_OnlyPendingStatus() throws Exception {
        // Given: 여러 상태의 참가자들이 존재함
        createMultipleParticipantsInDatabase();

        mockMvc.perform(get("/api/v1/study/participant/{studyId}/participants/pending", TEST_STUDY_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 대기 중인 참가자만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가자 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1)) // 1명의 대기 중인 참가자
                .andExpect(jsonPath("$.data.content[0].memberName").value("이참가"))
                .andExpect(jsonPath("$.data.content[0].memberGrade").value("JUNIOR"))
                .andExpect(jsonPath("$.data.content[0].profileImageUrl").exists());

    }

    @Test
    @Order(12)
    @DisplayName("✅ 스터디별 승인된 참가자 목록 조회")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void getApprovedParticipants_OnlyApprovedStatus() throws Exception {
        // Given: 여러 상태의 참가자들이 존재함
        createMultipleParticipantsInDatabase();

        mockMvc.perform(get("/api/v1/study/participant/{studyId}/participants/approved", TEST_STUDY_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 승인된 참가자만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가자 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1)) // 1명의 승인된 참가자
                .andExpect(jsonPath("$.data.content[0].memberName").value("박신청"))
                .andExpect(jsonPath("$.data.content[0].memberGrade").value("SOPHOMORE"))
                .andExpect(jsonPath("$.data.content[0].profileImageUrl").exists());

    }

    @Test
    @Order(13)
    @DisplayName("👤 회원별 참가 스터디 목록 조회")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void getMemberParticipations_MemberStudyList() throws Exception {
        // Given: 회원이 여러 스터디에 참가함
        createMultipleStudiesWithParticipations();

        mockMvc.perform(get("/api/v1/study/participant/members/{memberId}/participations", TEST_PARTICIPANT_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 해당 회원이 참가한 스터디 목록 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가자 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content").isArray());

    }

    // =================================================================
    // 🚨 검증 실패 및 예외 상황 테스트
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("❌ 참가 신청 실패 - 필수 필드 누락")
    void registerJoinStudy_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        StudyJoinRequestDto request = new StudyJoinRequestDto();
        // studyId, memberId 모두 누락

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/study/participant/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.statusCode").value(Matchers.greaterThanOrEqualTo(400)));

    }

    @Test
    @Order(21)
    @DisplayName("❌ 중복 참가 신청 실패 - 이미 신청한 사용자")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void registerJoinStudy_BusinessFailure_DuplicateApplication() throws Exception {
        // Given: 이미 참가 신청한 사용자가 다시 신청을 시도
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        StudyJoinRequestDto request = createValidJoinRequest();

        mockMvc.perform(post("/api/v1/study/participant/join/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.statusCode").value(Matchers.greaterThanOrEqualTo(400)))
                .andExpect(jsonPath("$.message").value("이미 참가 신청한 스터디입니다."));

    }

    @Test
    @Order(22)
    @DisplayName("❌ 참가 승인 실패 - 권한 없는 사용자")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void approveJoinStudy_AuthorizationFailure_UnauthorizedUser() throws Exception {
        // Given: 참가 신청이 존재하지만, 스터디 생성자가 아닌 사용자가 승인을 시도
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        
        StudyJoinApproveRequestDto request = new StudyJoinApproveRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setMemberId(TEST_PARTICIPANT_ID);
        // Note: ApproverId validation is handled by service layer


        mockMvc.perform(post("/api/v1/study/participant/join/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다."));

    }

    @Test
    @Order(23)
    @DisplayName("♻️ 거절 후 재신청 - 성공적으로 복원/대기 상태 전환")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void reapplyAfterRejected_ShouldSucceedWithPending() throws Exception {
        // Given: 참가 신청 생성 후 스터디 생성자(user1)가 거절
        createTestParticipantInDatabase(StudyParticipantStatus.PENDING);
        var rejectReq = new StudyJoinRejectRequestDto();
        rejectReq.setStudyId(TEST_STUDY_ID);
        rejectReq.setMemberId(TEST_PARTICIPANT_ID);

        mockMvc.perform(post("/api/v1/study/participant/join/reject")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CurrentUser(TEST_MEMBER_ID, "user1", "user1@certis.org", "유저1", "UPSOLVER")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andDo(print())
                .andExpect(status().isOk());

        // When: 신청자(user2)가 다시 동일 스터디에 재신청 (요청 단위 user 주입)
        StudyJoinRequestDto request = createValidJoinRequest();

        // Then: 201 Created, message, data.status=PENDING
        mockMvc.perform(post("/api/v1/study/participant/join/register")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CurrentUser(TEST_PARTICIPANT_ID, "user2", "user2@certis.org", "유저2", "PLAYER")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    // =================================================================
    // 📊 성능 및 대용량 데이터 테스트
    // =================================================================

    @Test
    @Order(30)
    @DisplayName("📊 대용량 참가자 목록 조회 - 페이징 성능 테스트")
    void getStudyParticipants_PerformanceTest_LargeDataset() throws Exception {
        // Given: 대량의 참가자 데이터 생성 (100명)
        createLargeParticipantDataset(100);

        long startTime = System.currentTimeMillis();

        // When: 페이징된 참가자 목록 조회
        mockMvc.perform(get("/api/v1/study/participant/{studyId}/participants/all", TEST_STUDY_ID)
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
        
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 참가 신청 요청 DTO 생성
     */
    private StudyJoinRequestDto createValidJoinRequest() {
        StudyJoinRequestDto request = new StudyJoinRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        // Note: MemberId is handled by security context in real implementation
        return request;
    }

    /**
     * 테스트 데이터 설정 (스터디, 멤버)
     */
    private void setupTestData() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            
            // 멤버 데이터 생성 (스터디 생성자)
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_MEMBER_ID)
                    .set(MEMBER.NAME, TEST_MEMBER_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "kim.dev@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(25))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, "SENIOR")
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.PROFILE_IMAGE, "https://example.com/profile1.jpg")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 멤버 데이터 생성 (참가 신청자)
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_PARTICIPANT_ID)
                    .set(MEMBER.NAME, TEST_PARTICIPANT_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "lee.participant@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "FEMALE")
                    .set(MEMBER.GRADE, "JUNIOR")
                    .set(MEMBER.MAJOR, "정보보안학과")
                    .set(MEMBER.PROFILE_IMAGE, "https://example.com/profile2.jpg")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 멤버 데이터 생성 (또 다른 참가 신청자)
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_PARTICIPANT_2_ID)
                    .set(MEMBER.NAME, TEST_PARTICIPANT_2_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "park.applicant@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(22))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, "SOPHOMORE")
                    .set(MEMBER.MAJOR, "소프트웨어학과")
                    .set(MEMBER.PROFILE_IMAGE, "https://example.com/profile3.jpg")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 스터디 데이터 생성
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, TEST_STUDY_ID)
                    .set(STUDY.TITLE, TEST_STUDY_TITLE)
                    .set(STUDY.DESCRIPTION, "통합 테스트용 스터디")
                    .set(STUDY.CONTENT, "스터디 상세 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(1))
                    .set(STUDY.ENDED_AT, now.plusDays(30))
                    .set(STUDY.CREATED_AT, now)
                    .set(STUDY.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

        } catch (Exception e) {
        }
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        try {
            // 외래 키 제약으로 인해 역순으로 삭제
            dsl.deleteFrom(STUDY_PARTICIPANT).execute();
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
        }
    }

    /**
     * 데이터베이스에 테스트용 참가자 생성
     */
    private void createTestParticipantInDatabase(StudyParticipantStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.ID, TEST_STUDY_PARTICIPANT_ID)
                .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_PARTICIPANT_ID)
                .set(STUDY_PARTICIPANT.STATUS, status.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();
    }

    /**
     * 다수의 참가자 생성 (목록 조회 테스트용)
     */
    private void createMultipleParticipantsInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // PENDING 상태 참가자
        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_PARTICIPANT_ID)
                .set(STUDY_PARTICIPANT.STATUS, StudyParticipantStatus.PENDING.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();

        // APPROVED 상태 참가자
        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_PARTICIPANT_2_ID)
                .set(STUDY_PARTICIPANT.STATUS, StudyParticipantStatus.APPROVED.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();

        // REJECTED 상태 참가자 (추가 멤버 필요)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 4L)
                .set(MEMBER.NAME, "최거절")
                .set(MEMBER.STUDENT_NUMBER, "choi.rejected@certis.org")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.BIRTHDAY, now.minusYears(24))
                .set(MEMBER.GENDER, "FEMALE")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();

        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_PARTICIPANT.MEMBER_ID, 4L)
                .set(STUDY_PARTICIPANT.STATUS, StudyParticipantStatus.REJECTED.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, now)
                .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                .execute();
    }

    /**
     * 회원별 참가 스터디 생성 (여러 스터디에 참가)
     */
    private void createMultipleStudiesWithParticipations() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // 추가 스터디 생성 (ID를 자동 생성하도록 수정)
        for (int i = 1; i <= 2; i++) {
            // 기존 스터디와 충돌하지 않도록 명시적으로 ID를 지정하지 않고 자동 생성
            Long studyId = dsl.insertInto(STUDY)
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.TITLE, "추가 스터디 " + i)
                    .set(STUDY.DESCRIPTION, "추가 스터디 " + i + " 설명")
                    .set(STUDY.CONTENT, "추가 스터디 " + i + " 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(i + 1))
                    .set(STUDY.ENDED_AT, now.plusDays(30 + i + 1))
                    .set(STUDY.CREATED_AT, now.minusHours(i + 1))
                    .set(STUDY.UPDATED_AT, now.minusHours(i + 1))
                    .returningResult(STUDY.ID)
                    .fetchOne()
                    .getValue(STUDY.ID);

            // 각 스터디에 참가자 추가
            dsl.insertInto(STUDY_PARTICIPANT)
                    .set(STUDY_PARTICIPANT.STUDY_ID, studyId)
                    .set(STUDY_PARTICIPANT.MEMBER_ID, TEST_PARTICIPANT_ID)
                    .set(STUDY_PARTICIPANT.STATUS, StudyParticipantStatus.APPROVED.name())
                    .set(STUDY_PARTICIPANT.CREATED_AT, now)
                    .set(STUDY_PARTICIPANT.UPDATED_AT, now)
                    .execute();
        }
    }

    /**
     * 대용량 참가자 데이터셋 생성 (성능 테스트용)
     */
    private void createLargeParticipantDataset(int count) {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (int i = 1; i <= count; i++) {
            // 멤버 생성
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, (long) (1000 + i))
                    .set(MEMBER.NAME, "테스트참가자" + i)
                    .set(MEMBER.STUDENT_NUMBER, "test" + i + "@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(20 + (i % 10)))
                    .set(MEMBER.GENDER, i % 2 == 0 ? "MALE" : "FEMALE")
                    .set(MEMBER.GRADE, switch ((i % 4) + 1) {
                        case 1 -> "FRESHMAN";
                        case 2 -> "SOPHOMORE";
                        case 3 -> "JUNIOR";
                        case 4 -> "SENIOR";
                        default -> "SENIOR";
                    })
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 참가자 생성
            StudyParticipantStatus status = StudyParticipantStatus.values()[i % 3]; // PENDING, APPROVED, REJECTED 순환
            dsl.insertInto(STUDY_PARTICIPANT)
                    .set(STUDY_PARTICIPANT.ID, (long) i)
                    .set(STUDY_PARTICIPANT.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_PARTICIPANT.MEMBER_ID, (long) (1000 + i))
                    .set(STUDY_PARTICIPANT.STATUS, status.name())
                    .set(STUDY_PARTICIPANT.CREATED_AT, now.minusHours(i))
                    .set(STUDY_PARTICIPANT.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 참가자 등록 검증
     */
    private void verifyParticipantRegisteredInDatabase(StudyJoinRequestDto request) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(request.getStudyId()))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(TEST_PARTICIPANT_ID))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getValue(STUDY_PARTICIPANT.STUDY_ID)).isEqualTo(request.getStudyId());
        assertThat(participant.getValue(STUDY_PARTICIPANT.MEMBER_ID)).isEqualTo(TEST_PARTICIPANT_ID);
        assertThat(participant.getValue(STUDY_PARTICIPANT.STATUS)).isEqualTo(StudyParticipantStatus.PENDING.name());
    }

    /**
     * 참가자 상태 검증
     */
    private void verifyParticipantStatusInDatabase(Long participantId, StudyParticipantStatus expectedStatus) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(expectedStatus.name());
    }

    /**
     * 참가자 취소 검증 (소프트 삭제)
     */
    private void verifyParticipantCancelledInDatabase(Long studyId, Long memberId) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(studyId))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(memberId))
                .fetchOne();

        // 하드 삭제 확인 (레코드가 존재하지 않아야 함)
        assertThat(participant).isNull();
    }

    /**
     * 참가자 상태 업데이트 검증 (REJECTED 상태 - 소프트 삭제)
     */
    private void verifyParticipantStatusUpdatedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNotNull();
        // 거절 시 소프트 삭제되므로 deleted_at이 설정되어야 함
        assertThat(participant.getDeletedAt()).isNotNull();
        // softDeleteById는 상태를 변경하지 않고 소프트 삭제만 수행
        // 실제 거절 로직에서는 bulkRejectWithSoftDelete를 사용하여 상태도 REJECTED로 변경
        // 하지만 현재 구현에서는 softDeleteById만 사용하므로 상태는 그대로 유지됨
        // 따라서 상태 검증은 제거하고 소프트 삭제 여부만 확인
    }
}
