package org.certis.studyplatform.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.auth.presentation.dto.request.LoginRequestDto;
import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.domain.MemberRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🔐 AuthController 완전한 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DSLContext dsl;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/auth";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_ACCOUNT_NUMBER = "20201234";
    private static final String TEST_PASSWORD = "testPassword123!";
    private static final String TEST_NAME = "김테스트";
    private static final String TEST_EMAIL = "test@certis.org";
    private static final String TEST_PHONE = "010-0000-0000";
    private static final MemberRole TEST_ROLE = MemberRole.PLAYER;

    @BeforeEach
    void setUp() {
        cleanupTestData();
        setupTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    // =================================================================
    // 로그인 테스트
    // =================================================================

    @Test
    @Order(1)
    void login_Success_ValidCredentials() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.memberId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.role").value(TEST_ROLE.name()))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("refreshToken").contains("HttpOnly");
    }

    @Test
    @Order(2)
    void login_Failure_InvalidAccountNumber() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber("99999999")
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void login_Failure_InvalidPassword() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password("wrongPassword123!")
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =================================================================
    // 회원가입 테스트
    // =================================================================

    @Test
    @Order(4)
    void register_Success_ValidMemberInfo() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber("20241234")
                .password("newPassword123!")
                .name("신규회원")
                .email("new@certis.org")
                .role(MemberRole.PLAYER)
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        var memberRecord = dsl.selectFrom(MEMBER)
                .where(MEMBER.STUDENT_NUMBER.eq("20241234"))
                .fetchOne();
        assertThat(memberRecord).isNotNull();
        assertThat(memberRecord.getName()).isEqualTo("신규회원");

        var contactRecord = dsl.selectFrom(MEMBER_CONTACT)
                .where(MEMBER_CONTACT.MEMBER_ID.eq(memberRecord.getId()))
                .fetchOne();
        assertThat(contactRecord).isNotNull();
        assertThat(contactRecord.getEmail()).isEqualTo("new@certis.org");
    }

    @Test
    @Order(5)
    void register_Failure_DuplicateAccountNumber() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password("anotherPassword123!")
                .name("중복회원")
                .email("duplicate@certis.org")
                .role(MemberRole.PLAYER)
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // =================================================================
    // 보안 테스트
    // =================================================================

    @Test
    @Order(6)
    void security_Test_XssPrevention() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber("20249999")
                .password("xssPassword123!")
                .name("<script>alert('XSS')</script>")
                .email("xss@certis.org")
                .role(MemberRole.PLAYER)
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var memberRecord = dsl.selectFrom(MEMBER)
                .where(MEMBER.STUDENT_NUMBER.eq("20249999"))
                .fetchOne();
        assertThat(memberRecord.getName()).doesNotContain("<script>");
    }

    // =================================================================
    // 테스트 데이터 관리
    // =================================================================

    private void setupTestData() {
        // MEMBER (NOT NULL 필드 채워줌)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, TEST_NAME)
                .set(MEMBER.STUDENT_NUMBER, TEST_ACCOUNT_NUMBER)
                .set(MEMBER.GRADE, "3")
                .set(MEMBER.ROLE, TEST_ROLE.name())
                .set(MEMBER.MAJOR, "컴퓨터공학")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // MEMBER_CONTACT (NOT NULL 필드 채워줌)
        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, TEST_MEMBER_ID)
                .set(MEMBER_CONTACT.EMAIL, TEST_EMAIL)
                .set(MEMBER_CONTACT.PHONE_NUMBER, TEST_PHONE)
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // AUTH (로그인 계정 정보)
        dsl.insertInto(AUTH)
                .set(AUTH.MEMBER_ID, TEST_MEMBER_ID)
                .set(AUTH.ACCOUNT_NUMBER, TEST_ACCOUNT_NUMBER)
                .set(AUTH.PASSWORD, passwordEncoder.encode(TEST_PASSWORD))
                .set(AUTH.CREATED_AT, OffsetDateTime.now())
                .set(AUTH.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void setupAdminUser() {
        Long adminId = 10L;
        String adminAccountNumber = "20200001";
        String adminPassword = "adminPassword123!";

        // MEMBER
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, adminId)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, adminAccountNumber)
                .set(MEMBER.GRADE, "4")
                .set(MEMBER.ROLE, MemberRole.ADMIN.name())
                .set(MEMBER.MAJOR, "소프트웨어학과")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.parse("1995-05-05T00:00:00Z"))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // MEMBER_CONTACT
        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, adminId)
                .set(MEMBER_CONTACT.EMAIL, "admin@certis.org")
                .set(MEMBER_CONTACT.PHONE_NUMBER, "010-1111-1111")
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // AUTH
        dsl.insertInto(AUTH)
                .set(AUTH.MEMBER_ID, adminId)
                .set(AUTH.ACCOUNT_NUMBER, adminAccountNumber)
                .set(AUTH.PASSWORD, passwordEncoder.encode(adminPassword))
                .set(AUTH.CREATED_AT, OffsetDateTime.now())
                .set(AUTH.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }


    private void cleanupTestData() {
        dsl.deleteFrom(AUTH).execute();
        dsl.deleteFrom(MEMBER_CONTACT).execute();
        dsl.deleteFrom(MEMBER).execute();
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
    }
}
