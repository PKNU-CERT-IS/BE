package org.certis.studyplatform.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudyController 완전 새로운 통합 테스트
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
@DisplayName("🚀 StudyController 새로운 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl; // JOOQ로 직접 데이터베이스 조작

    // 테스트 상수
    private static final Long TEST_STUDY_ID = 1L;
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_MEMBER_2_ID = 2L;
    
    private static final String TEST_STUDY_TITLE = "CERT-IS 학습 플랫폼 스터디";
    private static final String TEST_STUDY_DESCRIPTION = "풀스택 웹 개발 스터디";
    private static final String TEST_STUDY_CONTENT = "React, Spring Boot를 활용한 웹 개발 학습";
    private static final String TEST_MEMBER_NAME = "김개발";
    private static final String TEST_MEMBER_2_NAME = "이테스트";

    @BeforeEach
    void setUp() {
        System.out.println("🔧 테스트 데이터 설정 시작");
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
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
    @DisplayName("📝 스터디 생성 - 성공적인 비즈니스 시나리오")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void createStudy_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 스터디 생성 요청이 준비됨
        StudyCreateRequestDto request = createValidStudyRequest();

        // When: 스터디 생성 API를 호출
        mockMvc.perform(post("/api/v1/study/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("스터디가 성공적으로 생성되었습니다"));

        // Then: 데이터베이스에 스터디가 정상적으로 저장되었는지 검증
        verifyStudyCreatedInDatabase(request);
        
        System.out.println("✅ 스터디 생성 테스트 성공");
    }

    @Test
    @Order(2)
    @DisplayName("🔍 스터디 상세 조회 - 완전한 정보 반환")
    void getStudyDetail_CompleteInformationReturned() throws Exception {
        // Given: 스터디가 미리 생성되어 있음
        createTestStudyInDatabase();

        // When: 스터디 상세 조회 API 호출
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 완전한 스터디 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디를 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.title").value(TEST_STUDY_TITLE))
                .andExpect(jsonPath("$.data.description").value(TEST_STUDY_DESCRIPTION))
                .andExpect(jsonPath("$.data.content").value(TEST_STUDY_CONTENT))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        System.out.println("✅ 스터디 상세 조회 테스트 성공");
    }

    @Test
    @Order(3)
    @DisplayName("✏️ 스터디 수정 - 권한 있는 사용자의 성공적인 수정")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void updateStudy_AuthorizedUserSuccessfulUpdate() throws Exception {
        // Given: 스터디가 존재하고, 생성자가 수정을 요청함
        createTestStudyInDatabase();
        
        StudyUpdateRequestDto request = new StudyUpdateRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setTitle("수정된 스터디 제목");
        request.setDescription("수정된 스터디 설명");
        request.setContent("수정된 스터디 내용입니다.");
        request.setCategory("웹 개발");
        request.setSubCategory("풀스택");

        // When: 스터디 수정 API 호출
        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 정보가 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 수정 확인
        verifyStudyUpdatedInDatabase(request);
        
        System.out.println("✅ 스터디 수정 테스트 성공");
    }

    @Test
    @Order(4)
    @DisplayName("📋 스터디 목록 조회 - 페이징과 정렬 기능")
    void getAllStudies_PaginationAndSortingFeatures() throws Exception {
        // Given: 여러 개의 스터디가 존재함
        createMultipleStudiesInDatabase();

        // When: 페이징된 스터디 목록 조회
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andDo(print())
                // Then: 페이징된 결과와 메타데이터 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3)) // 3개 스터디
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

        System.out.println("✅ 스터디 목록 조회 테스트 성공");
    }

    @Test
    @Order(5)
    @DisplayName("🔍 스터디 고급 검색 - 키워드 및 필터 기능")
    void searchStudiesAdvanced_KeywordAndFilterFeatures() throws Exception {
        // Given: 다양한 스터디가 존재함
        createMultipleStudiesInDatabase();

        // When: 고급 검색 API 호출
        mockMvc.perform(get("/api/v1/study/search")
                        .param("keyword", "개발")
                        .param("category", "웹 개발")
                        .param("status", "READY")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 필터링된 검색 결과 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 검색을 성공적으로 완료했습니다"))
                .andExpect(jsonPath("$.data.content").isArray());

        System.out.println("✅ 스터디 고급 검색 테스트 성공");
    }

    @Test
    @Order(6)
    @DisplayName("🗑️ 스터디 삭제 - 권한 있는 사용자의 성공적인 삭제")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void deleteStudy_AuthorizedUserSuccessfulDeletion() throws Exception {
        // Given: 스터디가 존재하고, 생성자가 삭제를 요청함
        createTestStudyInDatabase();
        
        StudyDeleteRequestDto request = new StudyDeleteRequestDto();
        request.setStudyId(TEST_STUDY_ID);

        // When: 스터디 삭제 API 호출
        mockMvc.perform(delete("/api/v1/study/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디가 성공적으로 삭제되었습니다"));

        // Then: 데이터베이스에서 소프트 삭제 확인 (deletedAt 필드 설정)
        verifyStudyDeletedInDatabase(TEST_STUDY_ID);
        
        System.out.println("✅ 스터디 삭제 테스트 성공");
    }

    // =================================================================
    // 🚨 검증 실패 및 예외 상황 테스트
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("❌ 스터디 생성 실패 - 필수 필드 누락")
    void createStudy_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        StudyCreateRequestDto request = new StudyCreateRequestDto();
        // title, description, content, category, subCategory 모두 누락

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/study/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        System.out.println("✅ 필수 필드 누락 검증 테스트 성공");
    }

    @Test
    @Order(11)
    @DisplayName("❌ 스터디 조회 실패 - 존재하지 않는 스터디")
    void getStudyDetail_NotFound_NonExistentStudy() throws Exception {
        // Given: 존재하지 않는 스터디 ID
        Long nonExistentStudyId = 99999L;

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", nonExistentStudyId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("스터디를 찾을 수 없습니다: " + nonExistentStudyId));

        System.out.println("✅ 존재하지 않는 스터디 조회 테스트 성공");
    }

    // =================================================================
    // 🔧 엣지 케이스 및 기술적 테스트
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("🎭 잘못된 Content-Type - HTTP 415 Unsupported Media Type")
    void createStudy_TechnicalFailure_UnsupportedMediaType() throws Exception {
        // Given: 잘못된 Content-Type
        StudyCreateRequestDto request = createValidStudyRequest();

        // When & Then: HTTP 415 Unsupported Media Type 응답
        mockMvc.perform(post("/api/v1/study/create")
                        .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        System.out.println("✅ 잘못된 Content-Type 테스트 성공");
    }

    @Test
    @Order(21)
    @DisplayName("📊 대용량 스터디 목록 조회 - 페이징 성능 테스트")
    void getAllStudies_PerformanceTest_LargeDataset() throws Exception {
        // Given: 대량의 스터디 데이터 생성 (50개)
        createLargeStudyDataset(50);

        long startTime = System.currentTimeMillis();

        // When: 페이징된 목록 조회
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                // Then: 정상 응답과 성능 기준 만족
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(50));

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 성능 검증: 1초 이내 응답
        assertThat(executionTime).isLessThan(1000);
        
        System.out.println("✅ 대용량 데이터 페이징 성능 테스트 성공 - 실행시간: " + executionTime + "ms");
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 스터디 생성 요청 DTO 생성
     */
    private StudyCreateRequestDto createValidStudyRequest() {
        StudyCreateRequestDto request = new StudyCreateRequestDto();
        request.setTitle(TEST_STUDY_TITLE);
        request.setDescription(TEST_STUDY_DESCRIPTION);
        request.setContent(TEST_STUDY_CONTENT);
        request.setCategory("웹 개발");
        request.setSubCategory("풀스택");
        request.setStartDate(OffsetDateTime.now().plusDays(1));
        request.setEndDate(OffsetDateTime.now().plusDays(30));
        request.setMaxParticipants(10);
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
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
            System.out.println("테스트 데이터 정리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스에 테스트용 스터디 생성
     */
    private void createTestStudyInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, TEST_STUDY_TITLE)
                .set(STUDY.DESCRIPTION, TEST_STUDY_DESCRIPTION)
                .set(STUDY.CONTENT, TEST_STUDY_CONTENT)
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발")
                .set(STUDY.SUBCATEGORY, "풀스택")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(30))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
    }

    /**
     * 다수의 스터디 생성 (목록 조회 테스트용)
     */
    private void createMultipleStudiesInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (int i = 1; i <= 3; i++) {
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, (long) i)
                    .set(STUDY.TITLE, "스터디 " + i)
                    .set(STUDY.DESCRIPTION, "스터디 " + i + " 설명")
                    .set(STUDY.CONTENT, "스터디 " + i + " 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(i))
                    .set(STUDY.ENDED_AT, now.plusDays(30 + i))
                    .set(STUDY.CREATED_AT, now.minusHours(i))
                    .set(STUDY.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 대용량 스터디 데이터셋 생성 (성능 테스트용)
     */
    private void createLargeStudyDataset(int count) {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (int i = 1; i <= count; i++) {
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, (long) i)
                    .set(STUDY.TITLE, "대용량 테스트 스터디 " + i)
                    .set(STUDY.DESCRIPTION, "대용량 테스트용 스터디 설명 " + i)
                    .set(STUDY.CONTENT, "대용량 테스트용 스터디 내용 " + i)
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(i))
                    .set(STUDY.ENDED_AT, now.plusDays(30 + i))
                    .set(STUDY.CREATED_AT, now.minusHours(i))
                    .set(STUDY.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 스터디 생성 검증
     */
    private void verifyStudyCreatedInDatabase(StudyCreateRequestDto request) {
        var study = dsl.selectFrom(STUDY)
                .where(STUDY.TITLE.eq(request.getTitle()))
                .and(STUDY.DELETED_AT.isNull())
                .fetchOne();

        assertThat(study).isNotNull();
        assertThat(study.getTitle()).isEqualTo(request.getTitle());
        assertThat(study.getDescription()).isEqualTo(request.getDescription());
        assertThat(study.getContent()).isEqualTo(request.getContent());
    }

    /**
     * 스터디 수정 검증
     */
    private void verifyStudyUpdatedInDatabase(StudyUpdateRequestDto request) {
        var study = dsl.selectFrom(STUDY)
                .where(STUDY.ID.eq(request.getStudyId()))
                .and(STUDY.DELETED_AT.isNull())
                .fetchOne();

        assertThat(study).isNotNull();
        assertThat(study.getTitle()).isEqualTo(request.getTitle());
        assertThat(study.getDescription()).isEqualTo(request.getDescription());
        assertThat(study.getUpdatedAt()).isAfter(study.getCreatedAt());
    }

    /**
     * 스터디 삭제 검증 (소프트 삭제)
     */
    private void verifyStudyDeletedInDatabase(Long studyId) {
        var study = dsl.selectFrom(STUDY)
                .where(STUDY.ID.eq(studyId))
                .fetchOne();

        assertThat(study).isNotNull();
        assertThat(study.getDeletedAt()).isNotNull(); // 소프트 삭제 확인
    }
}
