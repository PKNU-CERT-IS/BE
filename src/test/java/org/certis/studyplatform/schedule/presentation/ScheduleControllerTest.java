package org.certis.studyplatform.schedule.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.schedule.presentation.dto.request.ClubRoomUsageRequestDto;
import org.certis.studyplatform.schedule.presentation.dto.request.ClubRoomUsageDeleteRequestDto;
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
 * ScheduleController 완전한 통합 테스트
 *
 * 🎯 테스트 특징:
 * - CQRS 패턴과 JOOQ 기반 아키텍처 완전 활용
 * - Clean Architecture 계층별 테스트 분리
 * - BDD 스타일 테스트 시나리오 (Given-When-Then)
 * - 실제 비즈니스 시나리오 기반 테스트 케이스
 * - 포괄적인 검증 및 엣지 케이스 커버
 * - 테스트 데이터 격리 및 독립성 보장
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
@DisplayName("🚀 ScheduleController 완전한 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_USER_2_ID = 2L;
    private static final Long TEST_ADMIN_ID = 99L;
    private static final Long TEST_SCHEDULE_ID = 1L;
    private static final Long TEST_SCHEDULE_2_ID = 2L;

    private static final String TEST_USER_NAME = "김스케줄";
    private static final String TEST_USER_2_NAME = "이테스트";
    private static final String TEST_ADMIN_NAME = "박관리자";
    private static final String TEST_SCHEDULE_TITLE = "동아리방 사용 신청";
    private static final String TEST_SCHEDULE_DESCRIPTION = "프로젝트 회의를 위한 동아리방 사용";
    private static final String TEST_SCHEDULE_TYPE = "WORKSHOP";
    private static final String TEST_SCHEDULE_PLACE = "동아리방";

    @BeforeEach
    void setUp() {
        System.out.println("🔧 테스트 데이터 설정 시작");
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE schedule_status RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE schedule RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        // 보안 컨텍스트 설정 (user1)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user1", "password")
        );

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
    // 🎯 성공 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("📝 동아리방 사용 신청 - 성공적인 비즈니스 시나리오")
    void createClubRoomUsage_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 동아리방 사용 신청 요청이 준비됨
        ClubRoomUsageRequestDto request = createValidClubRoomRequest();

        // When: 동아리방 사용 신청 API를 호출
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 생성되었습니다"));

        // Then: 데이터베이스에 스케줄이 정상적으로 저장되었는지 검증
        verifyScheduleCreatedInDatabase(request.getTitle(), request.getDescription());

        System.out.println("✅ 동아리방 사용 신청 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("🔍 승인된 스케줄 조회 - 월별 캘린더 데이터")
    void getAllApprovedScheduleRequests_MonthlyCalendarData() throws Exception {
        // Given: 승인된 스케줄이 미리 생성되어 있음
        createApprovedScheduleInDatabase();

        OffsetDateTime queryDate = OffsetDateTime.now();

        // When: 월별 승인된 스케줄 조회 API 호출
        mockMvc.perform(get("/api/v1/schedule/requests")
                        .param("date", queryDate.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 승인된 스케줄 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정을 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));

        System.out.println("✅ 승인된 스케줄 조회 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("📋 내 신청 조회 - 전체 상태별 스케줄")
    void getMyRequests_AllStatusSchedules() throws Exception {
        // Given: 다양한 상태의 내 스케줄들이 존재함
        createMySchedulesWithVariousStatus();

        // When: 내 신청 조회 API 호출
        mockMvc.perform(get("/api/v1/schedule/me/request"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 모든 상태의 스케줄 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정을 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)); // PENDING, APPROVED, REJECTED

        System.out.println("✅ 내 신청 조회 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("🗑️ 동아리방 사용 삭제 - 성공적인 삭제 시나리오")
    void deleteClubRoomUsage_SuccessfulDeletionScenario() throws Exception {
        // Given: 삭제할 스케줄이 존재함
        createPendingScheduleInDatabase();

        ClubRoomUsageDeleteRequestDto request = ClubRoomUsageDeleteRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_ID)
                .build();

        // When: 동아리방 사용 삭제 API 호출
        mockMvc.perform(delete("/api/v1/schedule/request/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("일정이 성공적으로 삭제되었습니다"));

        // Then: 데이터베이스에서 스케줄이 삭제되었는지 검증
        verifyScheduleDeletedInDatabase(TEST_SCHEDULE_ID);

        System.out.println("✅ 동아리방 사용 삭제 테스트 성공");
    }

    // =================================================================
    // ❌ 실패 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(5)
    @DisplayName("❌ 동아리방 사용 신청 실패 - 필수 필드 누락")
    void createClubRoomUsage_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        ClubRoomUsageRequestDto request = ClubRoomUsageRequestDto.builder()
                // title, description, startedAt, endedAt 누락
                .build();

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 필수 필드 누락 검증 테스트 성공");
    }

    @Test
    @Order(6)
    @DisplayName("❌ 동아리방 사용 신청 실패 - 과거 시간으로 신청")
    void createClubRoomUsage_DomainFailure_PastTimeSchedule() throws Exception {
        // Given: 과거 시간으로 스케줄 신청
        ClubRoomUsageRequestDto request = ClubRoomUsageRequestDto.builder()
                .startedAt(OffsetDateTime.now().minusDays(1)) // 과거 시간
                .endedAt(OffsetDateTime.now().minusHours(1))
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("입력 데이터 검증에 실패했습니다"));

        System.out.println("✅ 과거 시간 스케줄 생성 방지 테스트 성공");
    }

    @Test
    @Order(7)
    @DisplayName("❌ 동아리방 사용 신청 실패 - 시작시간이 종료시간보다 늦음")
    void createClubRoomUsage_DomainFailure_InvalidTimeOrder() throws Exception {
        // Given: 잘못된 시간 순서의 스케줄 신청
        OffsetDateTime baseTime = OffsetDateTime.now().plusDays(1);
        ClubRoomUsageRequestDto request = ClubRoomUsageRequestDto.builder()
                .startedAt(baseTime.plusHours(2)) // 종료시간보다 늦은 시작시간
                .endedAt(baseTime.plusHours(1))
                .build();

        // When & Then: 도메인 규칙 위반으로 HTTP 422 응답
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("입력 데이터 검증에 실패했습니다"));

        System.out.println("✅ 잘못된 시간 순서 검증 테스트 성공");
    }

    @Test
    @Order(8)
    @DisplayName("❌ 동아리방 사용 신청 실패 - 빈 제목")
    void createClubRoomUsage_ValidationFailure_EmptyTitle() throws Exception {
        // Given: 빈 제목의 스케줄 신청
        ClubRoomUsageRequestDto request = ClubRoomUsageRequestDto.builder()
                .title("") // 빈 제목
                .build();

        // When & Then: 검증 실패로 HTTP 422 응답
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("입력 데이터 검증에 실패했습니다"));

        System.out.println("✅ 빈 제목 검증 테스트 성공");
    }

    @Test
    @Order(9)
    @DisplayName("❌ 동아리방 사용 신청 실패 - 잘못된 데이터 형식")
    void createClubRoomUsage_ValidationFailure_InvalidDataFormat() throws Exception {
        // Given: 잘못된 JSON 형식
        String invalidJson = "{ \"title\": \"테스트\", \"invalidField\": }"; // 잘못된 JSON

        // When & Then: JSON 파싱 오류로 HTTP 400 응답
        mockMvc.perform(post("/api/v1/schedule/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 잘못된 데이터 형식 검증 테스트 성공");
    }

    @Test
    @Order(10)
    @DisplayName("❌ 승인된 스케줄 조회 실패 - 잘못된 날짜 파라미터")
    void getAllApprovedScheduleRequests_ValidationFailure_InvalidDateParameter() throws Exception {
        // Given: 잘못된 날짜 파라미터
        String invalidDate = "invalid-date-format";

        // When & Then: 파라미터 검증 실패로 HTTP 400 응답
        mockMvc.perform(get("/api/v1/schedule/requests")
                        .param("date", invalidDate))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 잘못된 날짜 파라미터 검증 테스트 성공");
    }

    @Test
    @Order(11)
    @DisplayName("❌ 동아리방 사용 삭제 실패 - 존재하지 않는 스케줄")
    void deleteClubRoomUsage_NotFound_NonExistentSchedule() throws Exception {
        // Given: 존재하지 않는 스케줄 ID
        ClubRoomUsageDeleteRequestDto request = ClubRoomUsageDeleteRequestDto.builder()
                .scheduleId(99999L)
                .build();

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(delete("/api/v1/schedule/request/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("스케줄 상태를 찾을 수 없습니다"));

        System.out.println("✅ 존재하지 않는 스케줄 삭제 테스트 성공");
    }

    @Test
    @Order(12)
    @DisplayName("❌ 동아리방 사용 삭제 실패 - 권한 없는 사용자")
    void deleteClubRoomUsage_AuthorizationFailure_UnauthorizedUser() throws Exception {
        // Given: 다른 사용자가 생성한 스케줄
        createScheduleByAnotherUser();

        ClubRoomUsageDeleteRequestDto request = ClubRoomUsageDeleteRequestDto.builder()
                .scheduleId(TEST_SCHEDULE_2_ID)
                .build();

        // When & Then: HTTP 403 Forbidden 응답
        mockMvc.perform(delete("/api/v1/schedule/request/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("스케줄 관리에 적절하지 않은 사용자입니다."));

        System.out.println("✅ 권한 없는 사용자 삭제 시도 테스트 성공");
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 동아리방 사용 요청 DTO 생성
     */
    private ClubRoomUsageRequestDto createValidClubRoomRequest() {
        return ClubRoomUsageRequestDto.builder()
                .title(TEST_SCHEDULE_TITLE)
                .description(TEST_SCHEDULE_DESCRIPTION)
                .type(TEST_SCHEDULE_TYPE)
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
                    .set(MEMBER.ID, TEST_USER_ID)
                    .set(MEMBER.NAME, TEST_USER_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "20210001")
                    .set(MEMBER.ROLE, MemberRole.UPSOLVER.name())
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE,  MemberGrade.SENIOR.name())
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
                    .set(MEMBER.GRADE,  MemberGrade.SENIOR.name())
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_ADMIN_ID)
                    .set(MEMBER.NAME, TEST_ADMIN_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "20210010")
                    .set(MEMBER.ROLE, MemberRole.ADMIN.name())
                    .set(MEMBER.BIRTHDAY, now.minusYears(23))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE,  MemberGrade.SENIOR.name())
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

        } catch (Exception e) {
            System.err.println("테스트 데이터 설정 중 오류: " + e.getMessage());
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
            System.err.println("테스트 데이터 정리 중 오류: " + e.getMessage());
        }
    }

    /**
     * 스케줄이 데이터베이스에 정상 생성되었는지 검증
     */
    private void verifyScheduleCreatedInDatabase(String title, String description) {
        var schedules = dsl.selectFrom(SCHEDULE)
                .where(SCHEDULE.MEMBER_ID.eq(TEST_USER_ID))
                .and(SCHEDULE.TITLE.eq(title))
                .fetch();

        assertThat(schedules).hasSize(1);

        var schedule = schedules.get(0);
        assertThat(schedule.getTitle()).isEqualTo(title);
        assertThat(schedule.getDescription()).isEqualTo(description);
        assertThat(schedule.getType()).isEqualTo(TEST_SCHEDULE_TYPE);

        // 스케줄 상태도 PENDING으로 생성되었는지 확인
        var scheduleStatus = dsl.selectFrom(SCHEDULE_STATUS)
                .where(SCHEDULE_STATUS.SCHEDULE_ID.eq(schedule.getId()))
                .fetchOne();

        assertThat(scheduleStatus).isNotNull();
        assertThat(scheduleStatus.getStatus()).isEqualTo("PENDING");
    }

    /**
     * 스케줄이 데이터베이스에서 삭제되었는지 검증
     */
    private void verifyScheduleDeletedInDatabase(Long scheduleId) {
        var schedule = dsl.selectFrom(SCHEDULE)
                .where(SCHEDULE.ID.eq(scheduleId))
                .fetchOne();

        assertThat(schedule).isNotNull(); // 행은 존재해야 함
        assertThat(schedule.getDeletedAt()).isNotNull(); // deleted_at 이 채워졌는지 확인
    }


    /**
     * 승인된 스케줄을 데이터베이스에 생성
     */
    private void createApprovedScheduleInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        var scheduleId = dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, TEST_SCHEDULE_TITLE)
                .set(SCHEDULE.DESCRIPTION, TEST_SCHEDULE_DESCRIPTION)
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
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
     * 대기중인 스케줄을 데이터베이스에 생성
     */
    private void createPendingScheduleInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, TEST_SCHEDULE_ID)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, TEST_SCHEDULE_TITLE)
                .set(SCHEDULE.DESCRIPTION, TEST_SCHEDULE_DESCRIPTION)
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
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
     * 다양한 상태의 내 스케줄들을 생성
     */
    private void createMySchedulesWithVariousStatus() {
        OffsetDateTime now = OffsetDateTime.now();

        // PENDING 스케줄
        Long pendingId = 101L;
        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, pendingId)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "대기중 스케줄")
                .set(SCHEDULE.DESCRIPTION, "승인 대기중인 스케줄")
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
                .set(SCHEDULE.STARTED_AT, now.plusDays(1))
                .set(SCHEDULE.ENDED_AT, now.plusDays(1).plusHours(1))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, pendingId)
                .set(SCHEDULE_STATUS.STATUS, "PENDING")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();

        // APPROVED 스케줄
        Long approvedId = 102L;
        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, approvedId)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "승인된 스케줄")
                .set(SCHEDULE.DESCRIPTION, "승인된 스케줄")
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
                .set(SCHEDULE.STARTED_AT, now.plusDays(2))
                .set(SCHEDULE.ENDED_AT, now.plusDays(2).plusHours(1))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, approvedId)
                .set(SCHEDULE_STATUS.STATUS, "APPROVED")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();

        // REJECTED 스케줄
        Long rejectedId = 103L;
        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, rejectedId)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_ID)
                .set(SCHEDULE.TITLE, "거절된 스케줄")
                .set(SCHEDULE.DESCRIPTION, "거절된 스케줄")
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
                .set(SCHEDULE.STARTED_AT, now.plusDays(3))
                .set(SCHEDULE.ENDED_AT, now.plusDays(3).plusHours(1))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, rejectedId)
                .set(SCHEDULE_STATUS.STATUS, "REJECTED")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();
    }

    /**
     * 다른 사용자가 생성한 스케줄 생성 (권한 테스트용)
     */
    private void createScheduleByAnotherUser() {
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(SCHEDULE)
                .set(SCHEDULE.ID, TEST_SCHEDULE_2_ID)
                .set(SCHEDULE.MEMBER_ID, TEST_USER_2_ID) // 다른 사용자
                .set(SCHEDULE.TITLE, "다른 사용자 스케줄")
                .set(SCHEDULE.DESCRIPTION, "다른 사용자가 생성한 스케줄")
                .set(SCHEDULE.TYPE, TEST_SCHEDULE_TYPE)
                .set(SCHEDULE.PLACE, TEST_SCHEDULE_PLACE)
                .set(SCHEDULE.STARTED_AT, now.plusDays(1))
                .set(SCHEDULE.ENDED_AT, now.plusDays(1).plusHours(2))
                .set(SCHEDULE.CREATED_AT, now)
                .set(SCHEDULE.UPDATED_AT, now)
                .execute();

        dsl.insertInto(SCHEDULE_STATUS)
                .set(SCHEDULE_STATUS.SCHEDULE_ID, TEST_SCHEDULE_2_ID)
                .set(SCHEDULE_STATUS.STATUS, "PENDING")
                .set(SCHEDULE_STATUS.UPDATED_AT, now)
                .execute();
    }
}