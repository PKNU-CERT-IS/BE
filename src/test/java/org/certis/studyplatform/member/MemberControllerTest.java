package org.certis.studyplatform.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.presentation.dto.request.MemberUpdateRequestDto;
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

import java.time.OffsetDateTime;
import java.util.List;

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
@DisplayName("🚀 MemberController 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final String BASE_URL = "/api/v1/member";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_MEMBER_2_ID = 2L;

    private static final String TEST_MEMBER_NAME = "김테스트";
    private static final String TEST_MEMBER_2_NAME = "이검색";
    private static final String TEST_MAJOR = "소프트웨어학과";
    private static final String TEST_MAJOR_2 = "컴퓨터공학과";

    @BeforeEach
    void setUp() {
        System.out.println("🔧 테스트 데이터 설정 시작");
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
    @DisplayName("🔍 회원 키워드 검색 - 이름으로 검색 성공")
    void searchMembersByKeyword_ByName_Success() throws Exception {
        // Given: 검색할 회원이 데이터베이스에 존재함

        // When: 이름으로 회원 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("search", "김테스트"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 검색 결과 확인
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("회원 검색을 성공적으로 완료했습니다"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value(TEST_MEMBER_NAME))
                .andExpect(jsonPath("$.data[0].email").exists())
                .andExpect(jsonPath("$.data[0].githubUrl").exists());

        System.out.println("✅ 이름 검색 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("🔍 회원 키워드 검색 - 전공으로 검색 성공")
    void searchMembersByKeyword_ByMajor_Success() throws Exception {
        // Given: 같은 전공의 회원들이 존재함

        // When: 전공으로 회원 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("search", "소프트웨어"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 전공 매칭 결과 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].major").value(TEST_MAJOR));

        System.out.println("✅ 전공 검색 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("🔍 회원 키워드 검색 - 기술스택으로 검색 성공")
    void searchMembersByKeyword_BySkills_Success() throws Exception {
        // Given: 특정 기술스택을 가진 회원이 존재함

        // When: 기술스택으로 회원 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("search", "Java"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].skills[0]").value("Java"))
                .andExpect(jsonPath("$.data[0].skills").isArray());

        System.out.println("✅ 기술스택 검색 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("🔍 회원 검색 - 학년 필터링 성공")
    void searchMembersByGrade_FilterByGrade_Success() throws Exception {
        // Given: 다양한 학년의 회원들이 존재함

        // When: 학년으로 필터링하여 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("grade", "4학년"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 해당 학년 회원만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].grade").value("4학년"));

        System.out.println("✅ 학년 필터링 테스트 성공");
    }

    @Test
    @Order(5)
    @DisplayName("🔍 회원 검색 - 역할 필터링 성공")
    void searchMembersByRole_FilterByRole_Success() throws Exception {
        // Given: 다양한 역할의 회원들이 존재함

        // When: 역할로 필터링하여 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("role", "PLAYER"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 해당 역할 회원만 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].role").value("PLAYER"));

        System.out.println("✅ 역할 필터링 테스트 성공");
    }

    @Test
    @Order(6)
    @DisplayName("🔍 회원 검색 - 복합 조건 검색 성공")
    void searchMembersByMultipleConditions_Success() throws Exception {
        // Given: 복합 조건에 맞는 회원이 존재함

        // When: 검색어 + 학년 + 역할로 복합 검색 API 호출
        mockMvc.perform(get(BASE_URL + "/keyword")
                        .param("search", "김")
                        .param("grade", "4학년")
                        .param("role", "PLAYER"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 모든 조건에 맞는 회원 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        System.out.println("✅ 복합 조건 검색 테스트 성공");
    }

    @Test
    @Order(7)
    @DisplayName("✏️ 회원 정보 수정 - 성공적인 업데이트")
    void updateMember_Success() throws Exception {
        // Given: 수정할 회원 정보가 준비됨
        MemberUpdateRequestDto request = createValidUpdateRequest();

        // When: 회원 정보 수정 API 호출
        mockMvc.perform(put(BASE_URL + "/" + TEST_MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 수정 성공 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("회원 정보가 성공적으로 갱신되었습니다"))
                .andExpect(jsonPath("$.data.name.value").value(request.getName()));

        // Then: 데이터베이스에서 실제 수정 확인
        verifyMemberUpdatedInDatabase(TEST_MEMBER_ID, request.getName());

        System.out.println("✅ 회원 정보 수정 테스트 성공");
    }

    // =================================================================
    // 🔧 테스트 헬퍼 메서드들
    // =================================================================

    private void setupTestData() {
        // Member 테이블 데이터 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, TEST_MEMBER_NAME)
                .set(MEMBER.STUDENT_NUMBER, "20201111")
                .set(MEMBER.GRADE, "4학년")
                .set(MEMBER.ROLE, MemberRole.PLAYER.name())
                .set(MEMBER.MAJOR, TEST_MAJOR)
                .set(MEMBER.SKILLS, new String[]{"Java", "Spring", "React"})
                .set(MEMBER.DESCRIPTION, "테스트 회원입니다")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(22))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // MemberContact 테이블 데이터 생성
        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, TEST_MEMBER_ID)
                .set(MEMBER_CONTACT.EMAIL, "test@example.com")
                .set(MEMBER_CONTACT.GITHUB_URL, "https://github.com/test")
                .set(MEMBER_CONTACT.LINKEDIN_URL, "https://linkedin.com/in/test")
                .set(MEMBER_CONTACT.PHONE_NUMBER, "010-1234-5678")
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 추가 테스트 회원들 생성
        createAdditionalTestMembers();
    }

    private void createAdditionalTestMembers() {
        // 두 번째 테스트 회원
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_2_ID)
                .set(MEMBER.NAME, TEST_MEMBER_2_NAME)
                .set(MEMBER.STUDENT_NUMBER, "20202222")
                .set(MEMBER.GRADE, "3학년")
                .set(MEMBER.ROLE, MemberRole.PLAYER.name())
                .set(MEMBER.MAJOR, TEST_MAJOR_2)
                .set(MEMBER.SKILLS, new String[]{"Python", "Django"})
                .set(MEMBER.DESCRIPTION, "두 번째 테스트 회원")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(21))
                .set(MEMBER.GENDER, "FEMALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER_CONTACT)
                .set(MEMBER_CONTACT.MEMBER_ID, TEST_MEMBER_2_ID)
                .set(MEMBER_CONTACT.EMAIL, "test2@example.com")
                .set(MEMBER_CONTACT.PHONE_NUMBER, "010-2234-5678")
                .set(MEMBER_CONTACT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private MemberUpdateRequestDto createValidUpdateRequest() {
        return new MemberUpdateRequestDto(
                "김수정됨",
                "수정된 프로필 이미지 URL",
                "4학년",
                MemberRole.ADMIN,
                "소프트웨어학과",
                "",
                List.of("Java", "Spring Boot", "React", "TypeScript")
                );
    }

    private void verifyMemberUpdatedInDatabase(Long memberId, String expectedName) {
        var record = dsl.selectFrom(MEMBER)
                .where(MEMBER.ID.eq(memberId))
                .fetchOne();

        assertThat(record).isNotNull();
        assertThat(record.getName()).isEqualTo(expectedName);
    }

    private void cleanupTestData() {
        dsl.deleteFrom(MEMBER_CONTACT).execute();
        dsl.deleteFrom(MEMBER).execute();
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
    }
}