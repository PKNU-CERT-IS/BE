package org.certis.studyplatform.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.study.presentation.dto.request.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;
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
import org.certis.studyplatform.shared.util.DateTimeUtils;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.hamcrest.Matchers.*;
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
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class, StudyControllerTest.MockConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml", properties = {
        "spring.task.scheduling.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
    private static final Long TEST_STUDY_ID = 2L;
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_MEMBER_2_ID = 2L;
    
    private static final String TEST_STUDY_TITLE = "CERT-IS 학습 플랫폼 스터디";
    private static final String TEST_STUDY_DESCRIPTION = "풀스택 웹 개발 스터디";
    private static final String TEST_STUDY_CONTENT = "React, Spring Boot를 활용한 웹 개발 학습";
    private static final String TEST_MEMBER_NAME = "김개발";
    private static final String TEST_MEMBER_2_NAME = "이테스트";

    @BeforeEach
    void setUp() {
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE study_attached RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        org.certis.studyplatform.shared.domain.service.ProgressStatusScheduler progressStatusScheduler() {
            return mock(org.certis.studyplatform.shared.domain.service.ProgressStatusScheduler.class);
        }
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
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
        
    }

    @Test
    @Order(2)
    @DisplayName("🔍 스터디 상세 조회 - 완전한 정보 반환")
    void getStudyDetail_CompleteInformationReturned() throws Exception {
        // Given: 스터디가 미리 생성되어 있음
        createTestStudyInDatabase();

        // When: 스터디 상세 조회 API 호출
        var mvcResult = mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 완전한 스터디 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디를 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.creatorId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.title").value(TEST_STUDY_TITLE))
                .andExpect(jsonPath("$.data.description").value(TEST_STUDY_DESCRIPTION))
                .andExpect(jsonPath("$.data.content").value(TEST_STUDY_CONTENT))
                // 추가 필드 검증: 참가자 수/최대 인원/첨부 스터디 목록
                .andExpect(jsonPath("$.data.currentParticipantNumber").exists())
                .andExpect(jsonPath("$.data.maxParticipantNumber").value(10))
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.resultSubmitStatus").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                // 프로필 이미지 URL 검증
                .andExpect(jsonPath("$.data.studyCreatorProfileImageUrl").exists())
                .andReturn();

        // And: DB의 member_id와 응답의 creatorId가 일치하는지 검증
        String content = mvcResult.getResponse().getContentAsString();
        long responseCreatorId = objectMapper.readTree(content).at("/data/creatorId").asLong();
        Long dbCreatorId = dsl.select(STUDY.MEMBER_ID)
                .from(STUDY)
                .where(STUDY.ID.eq(TEST_STUDY_ID))
                .fetchOne(STUDY.MEMBER_ID);
        assertThat(responseCreatorId).isEqualTo(dbCreatorId);

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
        
    }

    @Test
    @Order(3)
    @DisplayName("✏️ 스터디 수정 - attachments=null 이면 기존 첨부 유지")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void updateStudy_NullAttachments_ShouldKeepExisting() throws Exception {
        // Given: 스터디와 기존 첨부 존재
        createTestStudyInDatabase();
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, "old.txt")
                .set(STUDY_ATTACHED.TYPE, "text/plain")
                .set(STUDY_ATTACHED.SIZE, "10")
                .set(STUDY_ATTACHED.ATTACHED_URL, "https://s3.example.com/old.txt")
                .set(STUDY_ATTACHED.CREATED_AT, now)
                .set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();

        StudyUpdateRequestDto request = new StudyUpdateRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setTitle("제목유지");
        request.setDescription("설명유지");
        request.setAttachments(null); // 핵심: null 전달

        // When
        mockMvc.perform(put("/api/v1/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Then: 첨부 테이블에 기존 첨부가 유지되어야 함 (현재 구현에서는 null 전달 시 기존 첨부 유지)
        Integer count = dsl.fetchCount(STUDY_ATTACHED, STUDY_ATTACHED.STUDY_ID.eq(TEST_STUDY_ID));
        assertThat(count).isEqualTo(1);
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
                .andExpect(jsonPath("$.data.content[0].resultSubmitStatus").exists())
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

    }

    @Test
    @Order(5)
    @DisplayName("📋 스터디 목록 조회 응답 필드 - 참가자 수와 첨부 포함")
    void getAllStudies_ResponseContainsParticipantNumbersAndAttachments() throws Exception {
        // Given
        createMultipleStudiesInDatabase();

        // When & Then
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].currentParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].maxParticipantNumber").exists())
                .andExpect(jsonPath("$.data.content[0].attachments").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists());
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
                        .param("studyStatus", "READY")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 필터링된 검색 결과 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 검색을 성공적으로 완료했습니다"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].status").exists());

    }

    @Test
    @Order(5)
    @DisplayName("🔍 스터디 고급 검색 - page/size 누락 시 기본값 적용")
    void searchStudiesAdvanced_DefaultPaging_WhenNoPageSizeParams() throws Exception {
        // Given: 다양한 스터디가 존재함
        createMultipleStudiesInDatabase();

        // When: page/size 미전달
        mockMvc.perform(get("/api/v1/study/search")
                        .param("keyword", "개발")
                        .param("category", "웹 개발"))
                .andDo(print())
                // Then: 기본 페이징(page=0, size=10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
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

    }

    @Test
    @Order(12)
    @DisplayName("❌ 스터디 생성 실패 - attachments.type 가 잘못된 Enum 값이면 400 반환")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void createStudy_InvalidAttachmentType_ShouldReturnBadRequest() throws Exception {
        // Given: 잘못된 Enum 값을 포함한 원시 JSON 요청
        String nowStart = OffsetDateTime.now().plusDays(1).toString();
        String nowEnd = OffsetDateTime.now().plusDays(30).toString();
        String payload = "{" +
                "\"title\":\"INVALID TYPE TEST\"," +
                "\"subCategory\":\"NUMBER_THERORY\"," +
                "\"maxParticipants\":3," +
                "\"description\":\"desc\"," +
                "\"content\":\"content\"," +
                "\"category\":\"CS\"," +
                "\"startDate\":\"" + nowStart + "\"," +
                "\"endDate\":\"" + nowEnd + "\"," +
                "\"attachments\":[{" +
                "\"name\":\"bad.pdf\"," +
                "\"type\":\"PDFX\"," + // 존재하지 않는 Enum 값
                "\"size\":12345," +
                "\"attachedUrl\":\"data:application/pdf;base64,AAA\"" +
                "}]" +
                "}";

        // When & Then: Jackson이 Enum 변환 실패 → GlobalExceptionHandler 가 400으로 매핑
        mockMvc.perform(post("/api/v1/study/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.data.type").value("ENUM_VALUE_INVALID"));
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

        // 성능 검증: CI 환경 변동성을 고려하여 3초 이내 응답
        assertThat(executionTime).isLessThan(3000);
        
    }

    @Test
    @Order(300)
    @DisplayName("✅ 스터디 상세 조회 - 첨부파일이 존재하면 배열에 채워진다")
    void should_return_attachments_in_study_detail_when_exist() throws Exception {
        // Given
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, TEST_STUDY_TITLE)
                .set(STUDY.DESCRIPTION, TEST_STUDY_DESCRIPTION)
                .set(STUDY.CONTENT, TEST_STUDY_CONTENT)
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발")
                .set(STUDY.SUBCATEGORY, "풀스택")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(now))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(now), 4))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        createTestAttachedFileInDatabase(TEST_STUDY_ID, "spec.pdf", "pdf", "12345", "https://s3.example.com/spec.pdf");

        // When & Then
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.attachments[0].attachedUrl").value("https://s3.example.com/spec.pdf"));
    }

    @Test
    @Order(301)
    @DisplayName("✅ 스터디 목록 조회 - 첨부파일 배열이 포함된다")
    void should_return_attachments_in_study_list_when_exist() throws Exception {
        // Given
        OffsetDateTime now = OffsetDateTime.now();

        // 최신 정렬 기준에 따라 첫 번째 요소가 되도록 ID=1이 가장 최근
        for (int i = 1; i <= 3; i++) {
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, (long) i)
                    .set(STUDY.TITLE, "스터디 " + i)
                    .set(STUDY.DESCRIPTION, "스터디 " + i + " 설명")
                    .set(STUDY.CONTENT, "스터디 " + i + " 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(now.plusDays(i)))
                    .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(now.plusDays(i)), 4))
                    .set(STUDY.CREATED_AT, now.minusHours(i))
                    .set(STUDY.UPDATED_AT, now.minusHours(i))
                    .execute();
        }

        // ID=1 스터디에 첨부파일 추가 (목록 첫 요소가 됨)
        createTestAttachedFileInDatabase(1L, "list-spec.pdf", "pdf", "7777", "https://s3.example.com/list-spec.pdf");

        // When & Then
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments").isArray())
                .andExpect(jsonPath("$.data.content[0].attachments.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].attachments[0].attachedUrl").value("https://s3.example.com/list-spec.pdf"));
    }

    @Test
    @Order(302)
    @DisplayName("✅ 스터디 상세 조회 - 이미지 첨부가 있으면 thumbnailUrl이 채워진다")
    void should_return_thumbnailUrl_in_study_detail_when_image_attachment_exists() throws Exception {
        // Given
        OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, TEST_STUDY_TITLE)
                .set(STUDY.DESCRIPTION, TEST_STUDY_DESCRIPTION)
                .set(STUDY.CONTENT, TEST_STUDY_CONTENT)
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발")
                .set(STUDY.SUBCATEGORY, "풀스택")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.plusDays(1))
                .set(STUDY.ENDED_AT, now.plusDays(30))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // 이미지와 비이미지 첨부를 함께 추가했을 때 첫 이미지의 URL이 썸네일이 됨
        createTestAttachedFileInDatabase(TEST_STUDY_ID, "문서.pdf", "pdf", "10000", "https://s3.example.com/doc.pdf");
        createTestAttachedFileInDatabase(TEST_STUDY_ID, "썸네일.png", "png", "2048", "https://s3.example.com/thumb.png");

        // When & Then
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thumbnailUrl").value("https://s3.example.com/thumb.png"));
    }

    @Test
    @Order(303)
    @DisplayName("✅ 스터디 목록 조회 - 이미지 첨부가 있으면 thumbnailUrl이 채워진다")
    void should_return_thumbnailUrl_in_study_list_when_image_attachment_exists() throws Exception {
        // Given
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 1; i <= 2; i++) {
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, (long) i)
                    .set(STUDY.TITLE, "스터디 " + i)
                    .set(STUDY.DESCRIPTION, "스터디 " + i + " 설명")
                    .set(STUDY.CONTENT, "스터디 " + i + " 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(i))
                    .set(STUDY.ENDED_AT, now.plusDays(30 + i))
                    .set(STUDY.CREATED_AT, now.minusHours(i))
                    .set(STUDY.UPDATED_AT, now.minusHours(i))
                    .execute();
        }

        // ID=1은 이미지 첨부 포함, ID=2는 비이미지 첨부만
        createTestAttachedFileInDatabase(1L, "표지.jpg", "jpg", "4096", "https://s3.example.com/cover.jpg");
        createTestAttachedFileInDatabase(2L, "문서.pdf", "pdf", "10000", "https://s3.example.com/only-doc.pdf");

        // When & Then
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://s3.example.com/cover.jpg"))
                .andExpect(jsonPath("$.data.content[1].thumbnailUrl").doesNotExist());
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
        // 도메인 규칙: 시작일은 월요일이어야 함 → 다음 월요일로 지정
        // 도메인 유틸과 동일한 규칙을 사용해 시작/종료일 계산
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startMonday = DateTimeUtils.calculateStudyStartWeek(now);
        OffsetDateTime endSunday = DateTimeUtils.calculateEndWeek(startMonday, 4);
        request.setStartDate(startMonday);
        request.setEndDate(endSunday);
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
                    .set(MEMBER.PROFILE_IMAGE, "https://example.com/profile1.jpg")
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
                    .set(MEMBER.PROFILE_IMAGE, "https://example.com/profile2.jpg")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
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
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
        }
    }

    /**
     * 데이터베이스에 테스트용 스터디 생성
     */
    private void createTestStudyInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startMonday = DateTimeUtils.calculateStudyStartWeek(now);
        OffsetDateTime endSunday = DateTimeUtils.calculateEndWeek(startMonday, 4);
        
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, TEST_STUDY_TITLE)
                .set(STUDY.DESCRIPTION, TEST_STUDY_DESCRIPTION)
                .set(STUDY.CONTENT, TEST_STUDY_CONTENT)
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "웹 개발")
                .set(STUDY.SUBCATEGORY, "풀스택")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, startMonday)
                .set(STUDY.ENDED_AT, endSunday)
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
            OffsetDateTime startMonday = DateTimeUtils.calculateStudyStartWeek(now.plusDays(i));
            OffsetDateTime endSunday = DateTimeUtils.calculateEndWeek(startMonday, 4);
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, (long) i)
                    .set(STUDY.TITLE, "스터디 " + i)
                    .set(STUDY.DESCRIPTION, "스터디 " + i + " 설명")
                    .set(STUDY.CONTENT, "스터디 " + i + " 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, startMonday)
                    .set(STUDY.ENDED_AT, endSunday)
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
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                    .set(STUDY.STARTED_AT, now.plusDays(i))
                    .set(STUDY.ENDED_AT, now.plusDays(30 + i))
                    .set(STUDY.CREATED_AT, now.minusHours(i))
                    .set(STUDY.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    private void createTestAttachedFileInDatabase(Long studyId, String name, String type, String size, String url) {
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY_ATTACHED)
                .set(STUDY_ATTACHED.STUDY_ID, studyId)
                .set(STUDY_ATTACHED.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_ATTACHED.NAME, name)
                .set(STUDY_ATTACHED.TYPE, type)
                .set(STUDY_ATTACHED.SIZE, size)
                .set(STUDY_ATTACHED.ATTACHED_URL, url)
                .set(STUDY_ATTACHED.CREATED_AT, now)
                .set(STUDY_ATTACHED.UPDATED_AT, now)
                .execute();
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

    // =================================================================
    // 🆕 새로운 기능 테스트 (semester, status 필드 추가)
    // =================================================================

    @Test
    @Order(100)
    @DisplayName("✅ 스터디 상세 조회 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_study_detail() throws Exception {
        // Given: 새로운 필드들을 포함한 스터디가 존재하는 상태
        createStudyWithNewFields();

        // When: 스터디 상세 조회
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", String.valueOf(TEST_STUDY_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.studyCreatorName").exists())
                .andExpect(jsonPath("$.data.studyCreatorGrade").exists())
                .andExpect(jsonPath("$.data.semester").exists())
                .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    @Order(101)
    @DisplayName("✅ 스터디 목록 조회 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_study_list() throws Exception {
        // Given: 새로운 필드들을 포함한 스터디가 존재하는 상태
        createStudyWithNewFields();

        // When: 스터디 목록 조회
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.content[0].semester").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists());
    }

    @Test
    @Order(102)
    @DisplayName("✅ 스터디 검색 시 새로운 필드들이 올바르게 반환된다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_return_new_fields_in_study_search() throws Exception {
        // Given: 새로운 필드들을 포함한 스터디가 존재하는 상태
        createStudyWithNewFields();

        // When: 스터디 검색
        mockMvc.perform(get("/api/v1/study/search")
                        .param("keyword", "새로운 필드")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.content[0].semester").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists());
    }

    @Test
    @Order(103)
    @DisplayName("✅ 스터디 고급 검색 - semester 필드로 필터링이 가능하다")
    @WithMockUser(username = "testuser", roles = {"PLAYER"})
    void should_filter_by_semester_in_advanced_search() throws Exception {
        // Given: 특정 학기의 스터디가 존재하는 상태
        createStudyWithNewFields();

        // When: 학기로 고급 검색
        mockMvc.perform(get("/api/v1/study/search")
                        .param("semester", "2025-02")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.content[0].semester").value("2025-2"));
    }

    // =================================================================
    // 🛠 새로운 기능 테스트를 위한 헬퍼 메서드
    // =================================================================

    /**
     * 새로운 필드들을 포함한 스터디 생성
     */
    private void createStudyWithNewFields() {
        // Ensure member exists first
        setupTestData();
        OffsetDateTime fixedStart = OffsetDateTime.parse("2025-09-01T00:00:00+09:00");
        OffsetDateTime fixedEnd = OffsetDateTime.parse("2025-11-30T23:59:59+09:00");
        
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "새로운 필드 테스트 스터디")
                .set(STUDY.DESCRIPTION, "semester와 status 필드 테스트")
                .set(STUDY.CONTENT, "새로운 필드들이 올바르게 저장되는지 테스트")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.STARTED_AT, fixedStart)
                .set(STUDY.ENDED_AT, fixedEnd)
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, OffsetDateTime.now())
                .execute();
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

    

    @Test
    @Order(200)
    @DisplayName("✅ 스터디 상세 조회 - Status 값 검증 (진행 중)")
    void should_return_correct_study_status_in_detail() throws Exception {
        // Given
        Long studyId = TEST_STUDY_ID;
        
        // 스터디를 현재 진행 중인 상태로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId)
                .set(STUDY.TITLE, "진행 중인 스터디")
                .set(STUDY.DESCRIPTION, "현재 진행 중인 스터디")
                .set(STUDY.CONTENT, "진행 중인 스터디 상세 내용")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, now.minusDays(1)) // 1일 전 시작
                .set(STUDY.ENDED_AT, now.plusDays(30))   // 30일 후 종료
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
        
        // When & Then - 진행 중인 스터디 조회
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", String.valueOf(studyId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(studyId))
                .andExpect(jsonPath("$.data.status").value("INPROGRESS")); // 진행 중 상태 확인
    }

    @Test
    @Order(201)
    @DisplayName("✅ 스터디 목록 조회 - Status 값 검증")
    void should_return_correct_study_status_in_list() throws Exception {
        // Given
        
        // 스터디를 현재 진행 중인 상태로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "진행 중인 스터디")
                .set(STUDY.DESCRIPTION, "현재 진행 중인 스터디")
                .set(STUDY.CONTENT, "진행 중인 스터디 상세 내용")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, now.minusDays(1)) // 1일 전 시작
                .set(STUDY.ENDED_AT, now.plusDays(30))   // 30일 후 종료
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
        
        // When & Then - 스터디 목록 조회
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].status").value("INPROGRESS")); // 진행 중 상태 확인
    }

    @Test
    @Order(202)
    @DisplayName("✅ 완료된 스터디 - Status 값 검증")
    void should_return_completed_status_for_ended_study() throws Exception {
        // Given
        Long studyId = TEST_STUDY_ID;
        
        // 스터디를 완료된 상태로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId)
                .set(STUDY.TITLE, "완료된 스터디")
                .set(STUDY.DESCRIPTION, "완료된 스터디 설명")
                .set(STUDY.CONTENT, "완료된 스터디 상세 내용")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, now.minusDays(10)) // 10일 전 시작
                .set(STUDY.ENDED_AT, now.minusDays(1))    // 1일 전 종료
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
        
        // When & Then - 완료된 스터디 조회
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", String.valueOf(studyId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(studyId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED")); // 완료 상태 확인
    }

    @Test
    @Order(203)
    @DisplayName("✅ 준비 중인 스터디 - Status 값 검증")
    void should_return_ready_status_for_future_study() throws Exception {
        // Given
        Long studyId = TEST_STUDY_ID;
        
        // 스터디를 처음부터 미래 날짜로 생성
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId)
                .set(STUDY.TITLE, "미래 스터디")
                .set(STUDY.CONTENT, "미래에 시작될 스터디")
                .set(STUDY.DESCRIPTION, "미래 스터디 설명")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.MEMBER_ID, 1L)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.STARTED_AT, now.plusDays(10)) // 10일 후 시작
                .set(STUDY.ENDED_AT, now.plusDays(40))   // 40일 후 종료
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();
        
        // When & Then - 준비 중인 스터디 조회
        mockMvc.perform(get("/api/v1/study/detail")
                        .param("studyId", String.valueOf(studyId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(studyId))
                .andExpect(jsonPath("$.data.status").value("READY")); // 준비 중 상태 확인
    }

    @Test
    @Order(106)
    @DisplayName("✅ 스터디 고급 검색 - status 필터로 상태별로 필터링된다")
    void should_filter_by_status_in_advanced_search() throws Exception {
        // Given: 기존 데이터 정리 후 서로 다른 상태의 스터디 3개 생성
        dsl.deleteFrom(STUDY).execute(); // 기존 데이터 정리
        
        OffsetDateTime now = OffsetDateTime.now();

        // READY: 미래 시작/미래 종료 (도메인 규칙 기반 월/일 계산)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 101L)
                .set(STUDY.TITLE, "READY 스터디")
                .set(STUDY.DESCRIPTION, "미래 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.getNextMondayFrom(now))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.getNextMondayFrom(now), 4))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // INPROGRESS: 현재 진행 주간 (월~일 범위)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 102L)
                .set(STUDY.TITLE, "INPROGRESS 스터디")
                .set(STUDY.DESCRIPTION, "진행중 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.getNextMondayFrom(now).minusWeeks(1))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.getNextMondayFrom(now).minusWeeks(1), 1))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // COMPLETED: 과거 주간 완료 (지난 주 월~일)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 103L)
                .set(STUDY.TITLE, "COMPLETED 스터디")
                .set(STUDY.DESCRIPTION, "완료 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.getNextMondayFrom(now).minusWeeks(2))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.getNextMondayFrom(now).minusWeeks(2), 1))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // When & Then: READY 필터 (studyStatus 파라미터 사용)
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "READY")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(101L))
                .andExpect(jsonPath("$.data.content[0].status").value("READY"));

        // When & Then: INPROGRESS 필터 (studyStatus 파라미터 사용)
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "INPROGRESS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(102L))
                .andExpect(jsonPath("$.data.content[0].status").value("INPROGRESS"));

        // When & Then: COMPLETED 필터 (studyStatus 파라미터 사용)
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(103L))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));
    }

    @Test
    @Order(8)
    @DisplayName("📊 스터디 목록 조회 응답 필드 - 참가자 수와 첨부 포함")
    void getAllStudies_ResponseFieldsWithParticipantCountAndAttachments() throws Exception {
        // Given: 테스트 데이터 생성
        createMultipleStudiesInDatabase();

        // When: 스터디 목록 조회 API 호출
        mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: 참가자 수와 첨부파일 필드가 포함된 응답 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 검색을 성공적으로 완료했습니다"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].title").exists())
                .andExpect(jsonPath("$.data.content[0].currentParticipantNumber").exists()) // 참가자 수 필드 존재
                .andExpect(jsonPath("$.data.content[0].maxParticipantNumber").exists()) // 최대 참가자 수 필드 존재
                .andExpect(jsonPath("$.data.content[0].attachments").isArray()); // 첨부파일 배열 필드 존재
    }

    @Test
    @Order(107)
    @DisplayName("✅ 스터디 고급 검색 - 상태 매핑 로직 테스트 (READY → READY, APPROVED / INPROGRESS → INPROGRESS)")
    void should_filter_by_status_mapping_logic() throws Exception {
        // Given: 기존 데이터 정리 후 다양한 상태의 스터디 생성
        dsl.deleteFrom(STUDY).execute(); // 기존 데이터 정리
        
        OffsetDateTime now = OffsetDateTime.now();

        // READY 상태 스터디 (도메인 규칙 기반 시작 주)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 201L)
                .set(STUDY.TITLE, "READY 스터디")
                .set(STUDY.DESCRIPTION, "준비중 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(now))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(now), 4))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // APPROVED 상태 스터디 (READY 필터에서 포함되어야 함, 도메인 규칙 기반 시작 주 + 여유 주 추가)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 202L)
                .set(STUDY.TITLE, "APPROVED 스터디")
                .set(STUDY.DESCRIPTION, "승인된 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "APPROVED")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(now).plusWeeks(4))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(now).plusWeeks(4), 4))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // INPROGRESS 상태 스터디 (이번 주 월~일 범위)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 203L)
                .set(STUDY.TITLE, "INPROGRESS 스터디")
                .set(STUDY.DESCRIPTION, "진행중 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "INPROGRESS")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.getNextMondayFrom(now).minusWeeks(1))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.getNextMondayFrom(now).minusWeeks(1), 1))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // COMPLETED 상태 스터디 (지난 주 월~일)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, 204L)
                .set(STUDY.TITLE, "COMPLETED 스터디")
                .set(STUDY.DESCRIPTION, "완료 스터디")
                .set(STUDY.CONTENT, "내용")
                .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "백엔드")
                .set(STUDY.STATUS, "COMPLETED")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, DateTimeUtils.getNextMondayFrom(now).minusWeeks(2))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.getNextMondayFrom(now).minusWeeks(2), 1))
                .set(STUDY.CREATED_AT, now)
                .set(STUDY.UPDATED_AT, now)
                .execute();

        // When & Then: READY 필터 - READY와 APPROVED 상태 모두 반환
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "READY")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].status").value(anyOf(is("READY"), is("APPROVED"))))
                .andExpect(jsonPath("$.data.content[1].status").value(anyOf(is("READY"), is("APPROVED"))));

        // When & Then: INPROGRESS 필터 - INPROGRESS 상태만 반환
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "INPROGRESS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(203L))
                .andExpect(jsonPath("$.data.content[0].status").value("INPROGRESS"));

        // When & Then: COMPLETED 필터 - COMPLETED 상태만 반환
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(204L))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));
    }

    @Test
    @Order(108)
    @DisplayName("✅ 스터디 고급 검색 - 올바른 필드명 사용 시 성공")
    void should_succeed_when_using_correct_field_name() throws Exception {
        // Given: 테스트 데이터 생성
        createMultipleStudiesInDatabase();

        // When & Then: 올바른 필드명 'studyStatus' 사용 시 성공
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "READY")  // 올바른 필드명
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(109)
    @DisplayName("✅ 스터디 고급 검색 - studyStatus 필드명 사용 시 정상 동작")
    void should_work_correctly_with_correct_field_name() throws Exception {
        // Given: 테스트 데이터 생성
        createMultipleStudiesInDatabase();

        // When & Then: 올바른 필드명 'studyStatus' 사용 시 정상 동작
        mockMvc.perform(get("/api/v1/study/search")
                        .param("studyStatus", "READY")  // 올바른 필드명
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
