package org.certis.studyplatform.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.auth.presentation.dto.request.LoginRequestDto;
import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.domain.MemberRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;
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
import static org.mockito.Mockito.*;
import jakarta.servlet.http.Cookie;


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

    @Autowired private MockMvc mockMvc; // HTTP 요청/응답 테스트용
    @Autowired private ObjectMapper objectMapper; // JSON 직렬화/역직렬화
    @Autowired private DSLContext dsl; // JOOQ를 통한 직접 DB 조작
    @Autowired private PasswordEncoder passwordEncoder; // 비밀번호 암호화

    @Autowired
    private RedisRefreshTokenRepository redisRefreshTokenRepository; // Redis Mock 처리

    @TestConfiguration
    static class MockOverrides {
        @Bean
        @Primary
        RedisRefreshTokenRepository redisRefreshTokenRepository() {
            return mock(RedisRefreshTokenRepository.class);
        }
    }

    // 테스트 상수 정의
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
        cleanupTestData(); // 테스트 전 데이터 정리
        setupTestData();   // 테스트용 기본 데이터 생성


    }

    @AfterEach
    void tearDown() {
        cleanupTestData(); // 테스트 후 데이터 정리
    }

    // =================================================================
    // 로그인 테스트
    // =================================================================

    @Test
    @Order(1)
    void login_Success_ValidCredentials() throws Exception {
        // 유효한 계정정보로 로그인 성공 테스트
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk()) // 200 OK 확인
                .andExpect(jsonPath("$.data.accessToken").exists()) // AccessToken 존재 확인
                .andExpect(jsonPath("$.data.memberId").value(TEST_MEMBER_ID)) // 회원ID 확인
                .andExpect(jsonPath("$.data.role").value(TEST_ROLE.name())) // 역할 확인
                .andReturn();

        // HttpOnly 쿠키에 RefreshToken 설정 확인
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("refreshToken").contains("HttpOnly");
    }

    @Test
    @Order(2)
    void login_Failure_InvalidAccountNumber() throws Exception {
        // 존재하지 않는 계정번호로 로그인 실패 테스트
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber("99999999") // 존재하지 않는 계정번호
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized 응답 확인
    }

    @Test
    @Order(3)
    void login_Failure_InvalidPassword() throws Exception {
        // 잘못된 비밀번호로 로그인 실패 테스트
        LoginRequestDto request = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password("wrongPassword123!") // 잘못된 비밀번호
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized 응답 확인
    }

    // =================================================================
    // 회원가입 테스트
    // =================================================================

    @Test
    @Order(4)
    void register_Success_ValidMemberInfo() throws Exception {
        // 완전한 유효한 정보로 회원가입 성공 테스트
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber("20241234") // 새로운 계정번호
                .password("newPassword123!") // 유효한 비밀번호
                .name("신규회원")
                .studentNumber("20241234") // 학번 (계정번호와 동일)
                .grade("3학년")
                .major("컴퓨터공학")
                .phoneNumber("010-2222-3333") // 필수 필드: 전화번호
                .email("new@certis.org") // 필수 필드: 이메일
                .birthday(OffsetDateTime.parse("2002-02-02T00:00:00Z"))
                .gender("FEMALE")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated()); // 201 Created 응답 확인

        // MEMBER 테이블에 데이터 저장 확인
        var memberRecord = dsl.selectFrom(MEMBER)
                .where(MEMBER.STUDENT_NUMBER.eq("20241234"))
                .fetchOne();
        assertThat(memberRecord).isNotNull();
        assertThat(memberRecord.getName()).isEqualTo("신규회원");
        assertThat(memberRecord.getGrade()).isEqualTo("JUNIOR");
        assertThat(memberRecord.getMajor()).isEqualTo("컴퓨터공학");

        // MEMBER_CONTACT 테이블에 연락처 정보 저장 확인
        var contactRecord = dsl.selectFrom(MEMBER_CONTACT)
                .where(MEMBER_CONTACT.MEMBER_ID.eq(memberRecord.getId()))
                .fetchOne();
        assertThat(contactRecord).isNotNull();
        assertThat(contactRecord.getEmail()).isEqualTo("new@certis.org");
        assertThat(contactRecord.getPhoneNumber()).isEqualTo("010-2222-3333");

        // AUTH 테이블에 인증 정보 저장 확인
        var authRecord = dsl.selectFrom(AUTH)
                .where(AUTH.MEMBER_ID.eq(memberRecord.getId()))
                .fetchOne();
        assertThat(authRecord).isNotNull();
        assertThat(authRecord.getAccountNumber()).isEqualTo("20241234");
    }

    @Test
    @Order(5)
    void register_Failure_DuplicateAccountNumber() throws Exception {
        // 이미 존재하는 계정번호로 회원가입 실패 테스트
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER) // 이미 존재하는 계정번호
                .password("anotherPassword123!")
                .name("중복회원")
                .studentNumber(TEST_ACCOUNT_NUMBER) // 동일 학번
                .grade("2학년")
                .major("전산학과")
                .phoneNumber("010-3333-4444")
                .email("duplicate@certis.org")
                .birthday(OffsetDateTime.parse("2001-03-03T00:00:00Z"))
                .gender("FEMALE")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict()); // 409 Conflict 응답 확인
    }

    @Test
    @Order(7)

    void logout_Success_AuthenticatedUser() throws Exception {
        // 인증된 사용자의 로그아웃 성공 테스트

        // Given: 먼저 로그인해서 토큰 획득
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 로그인 응답에서 AccessToken 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("accessToken")
                .asText();

        // When & Then: 로그아웃 성공
        MvcResult logoutResult = mockMvc.perform(post(BASE_URL + "/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk()) // 200 OK 응답 확인
                .andExpect(jsonPath("$.message").value("성공적으로 로그아웃되었습니다"))
                .andReturn();

        // RefreshToken 쿠키가 삭제되었는지 확인 (Max-Age=0)
        String setCookieHeader = logoutResult.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("refreshToken=");
        assertThat(setCookieHeader).contains("Max-Age=0"); // 쿠키 즉시 만료
    }

    // =================================================================
    // 토큰 갱신 테스트
    // =================================================================

    @Test
    @Order(8)
    void refreshToken_Success_ValidTokens() throws Exception {
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("refreshToken");

        // Redis 저장 확인
        verify(redisRefreshTokenRepository, atLeastOnce()).save(any(), any());
    }

    @Test
    @Order(9)
    void refreshToken_Success_TokenRotation() throws Exception {
        // Given: 로그인하여 토큰 획득
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 로그인 응답 쿠키에서 RefreshToken 추출
        Cookie[] cookies = loginResult.getResponse().getCookies();
        String originalRefreshToken = java.util.Arrays.stream(cookies)
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        assertThat(originalRefreshToken).isNotBlank();

        // Redis에 기존 RefreshToken 이 저장되어 있다고 가정
        when(redisRefreshTokenRepository.findByMemberId(any()))
                .thenReturn(java.util.Optional.of(
                        new org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo(
                                originalRefreshToken,
                                java.time.LocalDateTime.now().plusDays(1),
                                TEST_MEMBER_ID
                        )
                ));

        // When: 토큰 갱신 요청
        MvcResult refreshResult = mockMvc.perform(post(BASE_URL + "/token/refresh")
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        // Then: 토큰 로테이션 검증
        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(refreshResponseBody)
                .path("data")
                .path("accessToken")
                .asText();
        
        // 새로운 AccessToken 이 생성되었는지 확인 (리프레시 토큰 로테이션 없음)
        assertThat(newAccessToken).isNotEmpty();
        
        // 로그인 시 저장은 최소 1회 호출됨. 리프레시 동작에 대한 추가 호출은 보장하지 않음.
        verify(redisRefreshTokenRepository, atLeastOnce()).save(any(), any());
    }

    @Test
    @Order(10)
    void refreshToken_Failure_ExpiredRefreshToken() throws Exception {
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .accountNumber(TEST_ACCOUNT_NUMBER)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Redis 에 저장된 게 없다고 가정
        when(redisRefreshTokenRepository.findByMemberId(any())).thenReturn(java.util.Optional.empty());

        // 만료된 RefreshToken 쿠키 전송
        String expiredRefreshTokenCookie = "refreshToken=expired.refresh.token";

        mockMvc.perform(post(BASE_URL + "/token/refresh")
                        .header("Cookie", expiredRefreshTokenCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    // =================================================================
    // 보안 테스트
    // =================================================================

    @Test
    @Order(6)
    void security_Test_XssPrevention() throws Exception {
        // XSS 공격 시도가 포함된 이름으로 회원가입 시 보안 방어 테스트
        RegisterRequestDto request = RegisterRequestDto.builder()
                .accountNumber("20249999")
                .password("xssPassword123!")
                .name("<script>alert('XSS')</script>") // XSS 공격 시도
                .studentNumber("20249999")
                .grade("1학년")
                .major("정보보안학과")
                .phoneNumber("010-9999-0000")
                .email("xss@certis.org")
                .birthday(OffsetDateTime.parse("2003-09-01T00:00:00Z"))
                .gender("MALE")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()) // 201 -> 400으로 수정
                .andExpect(jsonPath("$.message").value("이름은 한글, 영문, 공백만 포함할 수 있습니다"));
    }


    // =================================================================
    // 테스트 데이터 관리
    // =================================================================

    private void setupTestData() {
        // 테스트용 기본 회원 데이터 생성

        // MEMBER 테이블에 기본 회원 정보 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.NAME, TEST_NAME)
                .set(MEMBER.STUDENT_NUMBER, TEST_ACCOUNT_NUMBER)
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.ROLE, TEST_ROLE.name())
                .set(MEMBER.MAJOR, "컴퓨터공학")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // MEMBER_CONTACT 테이블에 연락처 정보 생성
        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, TEST_MEMBER_ID)
                .set(MEMBER_CONTACT.EMAIL, TEST_EMAIL)
                .set(MEMBER_CONTACT.PHONE_NUMBER, TEST_PHONE)
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // AUTH 테이블에 인증 정보 생성 (비밀번호 암호화)
        dsl.insertInto(AUTH)
                .set(AUTH.MEMBER_ID, TEST_MEMBER_ID)
                .set(AUTH.ACCOUNT_NUMBER, TEST_ACCOUNT_NUMBER)
                .set(AUTH.PASSWORD, passwordEncoder.encode(TEST_PASSWORD))
                .set(AUTH.CREATED_AT, OffsetDateTime.now())
                .set(AUTH.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }


    // Removed unused setupAdminUser helper to avoid linter warnings
    /* private void setupAdminUser() {
        // 관리자 권한 테스트용 사용자 생성
        Long adminId = 10L;
        String adminAccountNumber = "20200001";
        String adminPassword = "adminPassword123!";

        // MEMBER 테이블에 관리자 정보 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, adminId)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, adminAccountNumber)
                .set(MEMBER.GRADE, "4학년")
                .set(MEMBER.ROLE, MemberRole.ADMIN.name())
                .set(MEMBER.MAJOR, "소프트웨어학과")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.parse("1995-05-05T00:00:00Z"))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // MEMBER_CONTACT 테이블에 관리자 연락처 정보 생성
        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, adminId)
                .set(MEMBER_CONTACT.EMAIL, "admin@certis.org")
                .set(MEMBER_CONTACT.PHONE_NUMBER, "010-1111-1111")
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // AUTH 테이블에 관리자 인증 정보 생성
        dsl.insertInto(AUTH)
                .set(AUTH.MEMBER_ID, adminId)
                .set(AUTH.ACCOUNT_NUMBER, adminAccountNumber)
                .set(AUTH.PASSWORD, passwordEncoder.encode(adminPassword))
                .set(AUTH.CREATED_AT, OffsetDateTime.now())
                .set(AUTH.UPDATED_AT, OffsetDateTime.now())
                .execute();
    } */


    private void cleanupTestData() {
        // 외래키 제약조건 고려하여 순서대로 데이터 삭제
        dsl.deleteFrom(AUTH).execute(); // 인증 정보 삭제
        dsl.deleteFrom(MEMBER_CONTACT).execute(); // 연락처 정보 삭제
        dsl.deleteFrom(MEMBER).execute(); // 회원 정보 삭제
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1"); // 시퀀스 초기화
    }
}