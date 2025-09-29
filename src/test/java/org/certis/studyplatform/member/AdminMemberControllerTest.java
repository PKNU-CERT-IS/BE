package org.certis.studyplatform.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.presentation.dto.request.AdminMemberUpdateRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.GrantGracePeriodRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.PenaltyRequestDto;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🚀 AdminMemberController 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    // 테스트 상수
    private static final String BASE_URL = "/api/v1/admin/member";
    private static final Long TEST_ADMIN_ID = 1L;
    private static final Long TEST_TARGET_MEMBER_ID = 2L;

    private static final String TEST_ADMIN_NAME = "관리자";
    private static final String TEST_TARGET_MEMBER_NAME = "대상회원";

    @BeforeEach
    void setUp() {
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE member_penalty RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member_contact RESTART IDENTITY CASCADE");
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
    @DisplayName("👑 관리자 회원 필드 수정 - 권한 변경 성공")
    void updateMemberAdminFields_ChangeRole_Success() throws Exception {
        // Given: 관리자 권한 변경 요청이 준비됨
        AdminMemberUpdateRequestDto request = new AdminMemberUpdateRequestDto(
                TEST_TARGET_MEMBER_ID,
                MemberRole.STAFF, // 새로운 역할
                MemberGrade.JUNIOR// 새로운 학년
        );

        // When: 관리자 권한으로 회원 필드 수정 API 호출
        mockMvc.perform(post(BASE_URL + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("관리자권한으로 회원 프로필이 성공적으로 갱신되었습니다"))
                .andExpect(jsonPath("$.data.memberId").value(TEST_TARGET_MEMBER_ID))
                .andExpect(jsonPath("$.data.newRole").value("STAFF"));

        // Then: 데이터베이스에서 실제 수정 확인
        verifyMemberRoleUpdatedInDatabase(TEST_TARGET_MEMBER_ID, MemberRole.STAFF);

    }

    @Test
    @Order(2)
    @DisplayName("🔍 관리자 회원 검색 - 키워드 검색 성공")
    void searchMembersForAdmin_Success() throws Exception {
        // Given: 검색할 회원이 존재함

        // When: 관리자용 회원 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("keyword", "대상"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 검색 결과 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("관리자권한으로 회원 목록을 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value(TEST_TARGET_MEMBER_NAME));

    }

    @Test
    @Order(3)
    @DisplayName("⏰ 유예기간 부여 - 성공")
    void grantGracePeriod_Success() throws Exception {
        // Given: 유예기간 부여 요청이 준비됨
        GrantGracePeriodRequestDto request = new GrantGracePeriodRequestDto(
                TEST_TARGET_MEMBER_ID,
                OffsetDateTime.now().plusDays(30) // 30일 유예기간
        );

        // When: 유예기간 부여 API 호출
        mockMvc.perform(post(BASE_URL + "/grace-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("관리자권한으로 회원 유예기간이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 유예기간 설정 확인
        verifyGracePeriodInDatabase(TEST_TARGET_MEMBER_ID);

    }

    @Test
    @Order(4)
    @DisplayName("⚠️ 벌점 부여 - 성공")
    void assignPenalty_Success() throws Exception {
        // Given: 벌점 부여 요청이 준비됨
        PenaltyRequestDto request = new PenaltyRequestDto(
                TEST_TARGET_MEMBER_ID,
                5 // 5점 벌점
        );

        // When: 벌점 부여 API 호출
        mockMvc.perform(post(BASE_URL + "/penalty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("관리자권한으로 회원 벌점이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 벌점 설정 확인
        verifyPenaltyInDatabase(TEST_TARGET_MEMBER_ID, 5);

    }

    @Test
    @Order(5)
    @DisplayName("🗑️ 회원 삭제 - 성공")
    void deleteMember_Success() throws Exception {
        // Given: 삭제할 회원이 존재함

        // When: 회원 삭제 API 호출
        mockMvc.perform(delete(BASE_URL + "/" + TEST_TARGET_MEMBER_ID))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 삭제 성공 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("회원이 성공적으로 삭제되었습니다"));

        // Then: 데이터베이스에서 소프트 삭제 확인
        verifyMemberDeletedInDatabase(TEST_TARGET_MEMBER_ID);

    }

    // =================================================================
    // 🔧 테스트 헬퍼 메서드들
    // =================================================================

    private void setupTestData() {
        // 관리자 회원 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_ADMIN_ID)
                .set(MEMBER.NAME, TEST_ADMIN_NAME)
                .set(MEMBER.STUDENT_NUMBER, "20200001")
                .set(MEMBER.GRADE, MemberGrade.SENIOR.name())
                .set(MEMBER.ROLE, MemberRole.ADMIN.name())
                .set(MEMBER.MAJOR, "소프트웨어학과")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(23))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 대상 회원 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_TARGET_MEMBER_ID)
                .set(MEMBER.NAME, TEST_TARGET_MEMBER_NAME)
                .set(MEMBER.STUDENT_NUMBER, "20200002")
                .set(MEMBER.GRADE, MemberGrade.SOPHOMORE.name())
                .set(MEMBER.ROLE, MemberRole.UPSOLVER.name())
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "FEMALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER_PENALTY)
                .set(MEMBER_PENALTY.MEMBER_ID, TEST_TARGET_MEMBER_ID)
                .set(MEMBER_PENALTY.PENALTY_POINT, 0)
                .set(MEMBER_PENALTY.PENALTIED_AT, OffsetDateTime.now())
                .set(MEMBER_PENALTY.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void verifyMemberRoleUpdatedInDatabase(Long memberId, MemberRole expectedRole) {
        var record = dsl.selectFrom(MEMBER)
                .where(MEMBER.ID.eq(memberId))
                .fetchOne();

        assertThat(record).isNotNull();
        assertThat(record.getRole()).isEqualTo(expectedRole.name());
    }

    private void verifyGracePeriodInDatabase(Long memberId) {
        var record = dsl.selectFrom(MEMBER)
                .where(MEMBER.ID.eq(memberId))
                .fetchOne();

        assertThat(record).isNotNull();
        assertThat(record.getGracePeriod()).isNotNull();
    }

    private void verifyPenaltyInDatabase(Long memberId, Integer expectedPoints) {
        var record = dsl.selectFrom(MEMBER_PENALTY)
                .where(MEMBER_PENALTY.MEMBER_ID.eq(memberId))
                .fetchOne();

        assertThat(record).isNotNull();
        assertThat(record.getPenaltyPoint()).isEqualTo(expectedPoints);
    }

    private void verifyMemberDeletedInDatabase(Long memberId) {
        var record = dsl.selectFrom(MEMBER)
                .where(MEMBER.ID.eq(memberId))
                .fetchOne();

        assertThat(record).isNotNull();
        assertThat(record.getDeletedAt()).isNotNull(); // 소프트 삭제 확인
    }

    private void cleanupTestData() {
        dsl.deleteFrom(MEMBER_PENALTY).execute();
        dsl.deleteFrom(MEMBER_CONTACT).execute();
        dsl.deleteFrom(MEMBER).execute();
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
    }
}
