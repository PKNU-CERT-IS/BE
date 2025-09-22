package org.certis.studyplatform.schedule.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleCreateRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleUpdateRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.AdminScheduleDeleteRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminScheduleController 완전한 통합 테스트
 *
 * 🎯 테스트 특징:
 * - 관리자 권한 기반 스케줄 관리 API 테스트
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
@DisplayName("🚀 AdminScheduleController 완전한 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "admin",roles = {"ADMIN"})
class AdminScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final Long TEST_ADMIN_ID = 99L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_USER_2_ID = 2L;
    private static final Long TEST_SCHEDULE_ID = 1L;

    private static final String TEST_ADMIN_NAME = "박관리자";
    private static final String TEST_USER_NAME = "김사용자";
    private static final String TEST_USER_2_NAME = "이사용자";
    private static final String TEST_ADMIN_SCHEDULE_TITLE = "관리자 생성 스케줄";
    private static final String TEST_ADMIN_SCHEDULE_DESCRIPTION = "관리자가 직접 생성하는 정기 행사";
    private static final String TEST_ADMIN_SCHEDULE_TYPE = "MEETING";
    private static final String TEST_ADMIN_SCHEDULE_PLACE = "대강당";

    @BeforeEach
    void setUp() {
        System.out.println("🔧 관리자 테스트 데이터 설정 시작");
        setupTestData();
        System.out.println("✅ 관리자 테스트 데이터 설정 완료");
    }

    @AfterEach
    void tearDown() {
        System.out.println("🧹 관리자 테스트 데이터 정리 시작");
        cleanupTestData();
        System.out.println("✅ 관리자 테스트 데이터 정리 완료");
    }

    // =================================================================
    // 🎯 성공 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("📝 관리자 스케줄 생성 - 성공적인 비즈니스 시나리오")
    void createScheduleByAdmin_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 관리자 스케줄 생성 요청이 준비됨
        AdminScheduleCreateRequestDto request = createValidAdminScheduleRequest();

        // When: 관리자 스케줄 생성 API를 호출
        mockMvc.perform(post("/api/v1/admin/schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 생성되었습니다"));

        // Then: 데이터베이스에 스케줄이 APPROVED 상태로 저장되었는지 검증
        verifyAdminScheduleCreatedInDatabase(request.getTitle(), request.getDescription(), request.getType(), request.getPlace());

        System.out.println("✅ 관리자 스케줄 생성 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("📋 대기중인 신청 조회 - 관리자용 승인 대기 목록")
    void getPendingScheduleRequests_AdminApprovalList() throws Exception {
        // Given: 대기중인 스케줄 신청들이 존재함
        createPendingScheduleRequests();

        // When: 대기중인 신청 조회 API 호출
        mockMvc.perform(get("/api/v1/admin/schedule/requests"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 대기중인 신청들 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정을 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2)) // 2개의 대기중인 신청
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        System.out.println("✅ 대기중인 신청 조회 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("✅ 스케줄 승인 - 성공적인 승인 시나리오")
    void approveOrRejectScheduleRequest_SuccessfulApprovalScenario() throws Exception {
        // Given: 대기중인 스케줄이 존재함
        createPendingScheduleInDatabase();

        AdminScheduleUpdateRequestDto request = AdminScheduleUpdateRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .status("APPROVED")
                .build();

        // When: 스케줄 승인 API 호출
        mockMvc.perform(put("/api/v1/admin/schedule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 상태가 APPROVED로 변경되었는지 검증
        verifyScheduleStatusUpdated(TEST_SCHEDULE_ID, "APPROVED");

        System.out.println("✅ 스케줄 승인 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("❌ 스케줄 거절 - 성공적인 거절 시나리오")
    void approveOrRejectScheduleRequest_SuccessfulRejectionScenario() throws Exception {
        // Given: 대기중인 스케줄이 존재함
        createPendingScheduleInDatabase();

        AdminScheduleUpdateRequestDto request = AdminScheduleUpdateRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .status("REJECTED")
                .build();

        // When: 스케줄 거절 API 호출
        mockMvc.perform(put("/api/v1/admin/schedule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 상태가 REJECTED로 변경되었는지 검증
        verifyScheduleStatusUpdated(TEST_SCHEDULE_ID, "REJECTED");

        System.out.println("✅ 스케줄 거절 테스트 성공");
    }

    @Test
    @Order(5)
    @DisplayName("🗑️ 관리자 스케줄 삭제 - 성공적인 삭제 시나리오")
    void deleteSchedule_SuccessfulDeletionScenario() throws Exception {
        // Given: 삭제할 스케줄이 존재함
        createApprovedScheduleInDatabase();

        AdminScheduleDeleteRequestDto request = AdminScheduleDeleteRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .build();

        // When: 관리자 스케줄 삭제 API 호출
        mockMvc.perform(delete("/api/v1/admin/schedule/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 삭제되었습니다"));

        // Then: 데이터베이스에서 스케줄이 삭제되었는지 검증
        verifyScheduleDeletedInDatabase(TEST_SCHEDULE_ID);

        System.out.println("✅ 관리자 스케줄 삭제 테스트 성공");
    }

    // =================================================================
    // ❌ 실패 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(6)
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("❌ 관리자 스케줄 생성 실패 - 관리자 권한 없음")
    void createScheduleByAdmin_AuthorizationFailure_NotAdminUser() throws Exception {
        // Given: 일반 사용자 권한으로 관리자 API 접근
        AdminScheduleCreateRequestDto request = createValidAdminScheduleRequest();

        mockMvc.perform(post("/api/v1/admin/schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"));

        System.out.println("✅ 관리자 권한 체크 테스트 (미래 개선 필요)");
    }

    @Test
    @Order(7)
    @DisplayName("❌ 관리자 스케줄 생성 실패 - 필수 필드 누락")
    void createScheduleByAdmin_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        AdminScheduleCreateRequestDto request = AdminScheduleCreateRequestDto.builder()
                // title, description, type, place, startedAt, endedAt 누락
                .build();

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/admin/schedule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 관리자 스케줄 필수 필드 누락 검증 테스트 성공");
    }

    @Test
    @Order(8)
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("❌ 대기중인 신청 조회 실패 - 관리자 권한 없음")
    void getPendingScheduleRequests_AuthorizationFailure_NotAdminUser() throws Exception {
        // Given: 일반 사용자 권한으로 관리자 API 접근

        // When & Then: 관리자 권한이 아니므로 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/schedule/requests"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"));

        System.out.println("✅ 대기중인 신청 조회 권한 체크 테스트 성공");
    }

    @Test
    @Order(9)
    @DisplayName("❌ 스케줄 승인 실패 - 존재하지 않는 스케줄")
    void approveOrRejectScheduleRequest_NotFound_NonExistentSchedule() throws Exception {
        // Given: 존재하지 않는 스케줄 ID
        AdminScheduleUpdateRequestDto request = AdminScheduleUpdateRequestDto.builder()
                .scheduleId(99999L)
                .status("APPROVED")
                .build();

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(put("/api/v1/admin/schedule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("스케줄 상태를 찾을 수 없습니다"));

        System.out.println("✅ 존재하지 않는 스케줄 승인 시도 테스트 성공");
    }

    @Test
    @Order(10)
    @DisplayName("❌ 스케줄 승인 실패 - 이미 처리된 요청")
    void approveOrRejectScheduleRequest_BusinessFailure_AlreadyProcessedRequest() throws Exception {
        // Given: 이미 승인된 스케줄
        createApprovedScheduleInDatabase();

        AdminScheduleUpdateRequestDto request = AdminScheduleUpdateRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .status("APPROVED")
                .build();

        // When & Then: 비즈니스 로직에 의해 HTTP 422 응답
        mockMvc.perform(put("/api/v1/admin/schedule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.message").value("대기 상태가 아닌 스케줄은 처리할 수 없습니다"));

        System.out.println("✅ 이미 처리된 스케줄 재처리 시도 테스트 성공");
    }

    @Test
    @Order(11)
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("❌ 스케줄 처리 실패 - 관리자 권한 없음")
    void approveOrRejectScheduleRequest_AuthorizationFailure_NotAdminUser() throws Exception {
        // Given: 대기중인 스케줄이 존재함
        createPendingScheduleInDatabase();

        AdminScheduleUpdateRequestDto request = AdminScheduleUpdateRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .status("APPROVED")
                .build();

        // When & Then: 관리자 권한이 아니므로 403 Forbidden
        mockMvc.perform(put("/api/v1/admin/schedule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"));

        System.out.println("✅ 스케줄 처리 권한 체크 테스트 성공");
    }

    @Test
    @Order(12)
    @DisplayName("❌ 관리자 스케줄 삭제 실패 - 존재하지 않는 스케줄")
    void deleteSchedule_NotFound_NonExistentSchedule() throws Exception {
        // Given: 존재하지 않는 스케줄 ID
        AdminScheduleDeleteRequestDto request = AdminScheduleDeleteRequestDto.builder()
                .scheduleId(99999L)
                .build();

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(delete("/api/v1/admin/schedule/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("스케줄 상태를 찾을 수 없습니다"));

        System.out.println("✅ 존재하지 않는 스케줄 삭제 시도 테스트 성공");
    }

    @Test
    @Order(13)
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("❌ 관리자 스케줄 삭제 실패 - 관리자 권한 없음")
    void deleteSchedule_AuthorizationFailure_NotAdminUser() throws Exception {
        // Given: 삭제할 스케줄이 존재함
        createApprovedScheduleInDatabase();

        AdminScheduleDeleteRequestDto request = AdminScheduleDeleteRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .build();

        // When & Then: 관리자 권한이 아니므로 403 Forbidden
        mockMvc.perform(delete("/api/v1/admin/schedule/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"));

        System.out.println("✅ 스케줄 삭제 권한 체크 테스트 성공");
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 관리자 스케줄 생성 요청 DTO 생성
     */
    private AdminScheduleCreateRequestDto createValidAdminScheduleRequest() {
        return AdminScheduleCreateRequestDto.builder()
                .title(TEST_ADMIN_SCHEDULE_TITLE)
                .description(TEST_ADMIN_SCHEDULE_DESCRIPTION)
                .type(TEST_ADMIN_SCHEDULE_TYPE)
                .place(TEST_ADMIN_SCHEDULE_PLACE)
                .startedAt(OffsetDateTime.now().plusDays(1))
                .endedAt(OffsetDateTime.now().plusDays(1).plusHours(2))
                .build();
    }

    /**
     * 테스트 데이터 설정 (Member, Schedule, ScheduleStatus)
     */
    private void setupTestData() {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            // Member 데이터 생성
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_ADMIN_ID)
                    .set(MEMBER.NAME, TEST_ADMIN_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "20210010")
                    .set(MEMBER.ROLE, MemberRole.ADMIN.name())
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, MemberGrade.SENIOR.name())
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_USER_ID)
                    .set(MEMBER.NAME, TEST_USER_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "20210001")
                    .set(MEMBER.ROLE, MemberRole.UPSOLVER.name())
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, MemberGrade.SENIOR.name())
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_USER_2_ID)
                    .set(MEMBER.NAME, TEST_USER_2_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "20210002")
                    .set(MEMBER.ROLE, MemberRole.PLAYER.name())
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, MemberGrade.SENIOR.name())
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

        } catch (Exception e) {
            System.err.println("관리자 테스트 데이터 설정 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        try {
            // 외래키 제약으로 인해 순서 중요: schedule_status → schedule → member
            dsl.deleteFrom(SCHEDULE_STATUS).execute();
            dsl.deleteFrom(SCHEDULE).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
            System.err.println("관리자 테스트 데이터 정리 중 오류: " + e.getMessage());
        }
    }

    /**
     * 관리자 스케줄이 데이터베이스에 정상 생성되었는지 검증
     */
    private void verifyAdminScheduleCreatedInDatabase(String title, String description, String type, String place) {
        var schedules = dsl.selectFrom(SCHEDULE)
                .where(SCHEDULE.TITLE.eq(title))
                .fetch();

        assertThat(schedules).hasSize(1);

        var schedule = schedules.get(0);
        assertThat(schedule.getTitle()).isEqualTo(title);
        assertThat(schedule.getDescription()).isEqualTo(description);
        assertThat(schedule.getType()).isEqualTo(type);
        assertThat(schedule.getPlace()).isEqualTo(place);

        // 관리자 생성 스케줄은 자동으로 APPROVED 상태
        var scheduleStatus = dsl.selectFrom(SCHEDULE_STATUS)
                .where(SCHEDULE_STATUS.SCHEDULE_ID.eq(schedule.getId()))
                .fetchOne();

        assertThat(scheduleStatus).isNotNull();
        assertThat(scheduleStatus.getStatus()).isEqualTo("APPROVED");
    }

    /**
     * 스케줄 상태가 정상적으로 업데이트되었는지 검증
     */
    private void verifyScheduleStatusUpdated(Long scheduleId, String expectedStatus) {
        var scheduleStatus = dsl.selectFrom(SCHEDULE_STATUS)
                .where(SCHEDULE_STATUS.SCHEDULE_ID.eq(scheduleId))
                .fetchOne();

        assertThat(scheduleStatus).isNotNull();
        assertThat(scheduleStatus.getStatus()).isEqualTo(expectedStatus);
    }

    /**
     * 스케줄이 데이터베이스에서 삭제되었는지 검증
     */
    private void verifyScheduleDeletedInDatabase(Long scheduleId) {
        var schedule = dsl.selectFrom(SCHEDULE)
                .where(SCHEDULE.ID.eq(scheduleId))
                .fetchOne();

        assertThat(schedule).isNotNull();
        assertThat(schedule.getDeletedAt()).isNotNull(); // Soft delete 확인
    }
    /**
     * 대기중인 스케줄을 데이터베이스에 생성
     */
    private void createPendingScheduleInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "대기중인 스케줄")
                .set(SCHEDULE.DESCRIPTION, "승인 대기중인 스케줄")
                .set(SCHEDULE.TYPE, "CLUB_ROOM")
                .set(SCHEDULE.PLACE, "동아리방")
                .set(SCHEDULE.STARTED_AT, now.plusDays(1))
                .set(SCHEDULE.ENDED_AT, now.plusDays(1).plusHours(2))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE_STATUS.STATUS, "PENDING")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * 승인된 스케줄을 데이터베이스에 생성
     */
    private void createApprovedScheduleInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "승인된 스케줄")
                .set(SCHEDULE.DESCRIPTION, "이미 승인된 스케줄")
                .set(SCHEDULE.TYPE, "CLUB_ROOM")
                .set(SCHEDULE.PLACE, "동아리방")
                .set(SCHEDULE.STARTED_AT, now.plusDays(1))
                .set(SCHEDULE.ENDED_AT, now.plusDays(1).plusHours(2))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE_STATUS.STATUS, "APPROVED")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * 여러 대기중인 스케줄 요청 생성 (관리자 조회용)
     */
    private void createPendingScheduleRequests() {
        OffsetDateTime now = OffsetDateTime.now();

        // 첫 번째 대기중인 요청
        Long pendingId1 = 101L;
        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, pendingId1)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "첫 번째 대기 요청")
                .set(SCHEDULE.DESCRIPTION, "첫 번째 승인 대기 요청")
                .set(SCHEDULE.TYPE, "CLUB_ROOM")
                .set(SCHEDULE.PLACE, "동아리방")
                .set(SCHEDULE.STARTED_AT, now.plusDays(1))
                .set(SCHEDULE.ENDED_AT, now.plusDays(1).plusHours(1))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, pendingId1)
                .set(SCHEDULE_STATUS.STATUS, "PENDING")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();

        // 두 번째 대기중인 요청
        Long pendingId2 = 102L;
        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, pendingId2)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_2_ID)
                .set(SCHEDULE.TITLE, "두 번째 대기 요청")
                .set(SCHEDULE.DESCRIPTION, "두 번째 승인 대기 요청")
                .set(SCHEDULE.TYPE, "STUDY_ROOM")
                .set(SCHEDULE.PLACE, "스터디룸")
                .set(SCHEDULE.STARTED_AT, now.plusDays(2))
                .set(SCHEDULE.ENDED_AT, now.plusDays(2).plusHours(1))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, pendingId2)
                .set(SCHEDULE_STATUS.STATUS, "PENDING")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();
    }
}