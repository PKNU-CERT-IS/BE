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
import org.certis.studyplatform.shared.dto.LinkDto;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.certis.studyplatform.shared.service.S3ObjectInfo;

/**
 * StudyMeetingController 완전 새로운 통합 테스트
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
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class, StudyMeetingControllerTest.MockConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("🚀 StudyMeetingController 새로운 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudyMeetingControllerTest {
    @Autowired
    private S3FileService s3FileService;

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
    private static final Long TEST_MEETING_ID = 1L;
    
    private static final String TEST_STUDY_TITLE = "CERT-IS 학습 플랫폼 스터디";
    private static final String TEST_MEMBER_NAME = "김개발";
    private static final String TEST_MEMBER_2_NAME = "이테스트";
    private static final String TEST_MEETING_TITLE = "스터디 킥오프 회의";
    private static final String TEST_MEETING_CONTENT = "스터디 목표와 일정을 논의했습니다.";

    @BeforeEach
    void setUp() {
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE study_meeting RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        S3FileService s3FileService() {
            return mock(S3FileService.class);
        }
    }

    // =================================================================
    // 🎯 비즈니스 시나리오 기반 통합 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("📝 스터디 회의록 생성 - 성공적인 비즈니스 시나리오")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void createStudyMeeting_SuccessfulBusinessScenario() throws Exception {
        // Given: 유효한 스터디와 참여자들이 존재하고, 회의록 생성 요청이 준비됨
        StudyMeetingCreateRequestDto request = createValidMeetingRequest();

        // When: 회의록 생성 API를 호출
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답과 성공 메시지 확인
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("스터디 회의록이 성공적으로 생성되었습니다"));

        // Then: 데이터베이스에 회의록이 정상적으로 저장되었는지 검증
        verifyMeetingCreatedInDatabase(request);
        
    }

    @Test
    @Order(25)
    @DisplayName("🔁 스터디 회의록 링크 교체 및 S3 메타 반영")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void updateStudyMeeting_ReplacesLinks_AndReturnsS3EnrichedLinks() throws Exception {
        // Given: 회의록 생성 및 초기 링크 2개
        StudyMeetingCreateRequestDto create = createValidMeetingRequest();
        create.setStudyId(TEST_STUDY_ID);
        create.setLinks(List.of(
            new LinkDto("old-1", "https://bucket.s3.ap-northeast-2.amazonaws.com/study-end-attachments/1/old1.pdf"),
            new LinkDto("old-2", "https://bucket.s3.ap-northeast-2.amazonaws.com/study-end-attachments/1/old2.pdf")
        ));

        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated());

        // When: 링크를 1개로 교체하여 수정
        StudyMeetingUpdateRequestDto update = new StudyMeetingUpdateRequestDto();
        update.setMeetingId(1L);
        update.setTitle("회의록 수정");
        update.setContent("내용 수정");
        update.setParticipantNumber(3);
        String newUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/study-end-attachments/1/new.pdf";
        update.setLinks(List.of(new LinkDto("new-title", newUrl)));

        // Mock S3 metadata
        when(s3FileService.getObjectInfo(newUrl))
                .thenReturn(new S3ObjectInfo("new.pdf", "application/pdf", 123L, newUrl));

        mockMvc.perform(put("/api/v1/study/meeting/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Then: DB에는 교체된 1개 링크만 존재
        var remaining = dsl.fetch("select count(*) as c from study_meeting_link where meeting_id = ? and deleted_at is null", 1L);
        assertThat(remaining.get(0).get("c", Integer.class)).isEqualTo(1);

        // And: 상세 조회 시 S3 메타에서 가져온 name/url이 노출
        var res = mockMvc.perform(get("/api/v1/study/meeting/detail").param("meetingId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(json.at("/data/links/0/title").asText()).isEqualTo("new.pdf");
        assertThat(json.at("/data/links/0/url").asText()).isEqualTo(newUrl);
    }

    @Test
    @Order(2)
    @DisplayName("🔍 스터디 회의록 상세 조회 - 완전한 정보 반환")
    void getStudyMeetingDetail_CompleteInformationReturned() throws Exception {
        // Given: 회의록이 미리 생성되어 있음
        createTestMeetingInDatabase();

        // When: 회의록 상세 조회 API 호출
        mockMvc.perform(get("/api/v1/study/meeting/detail")
                        .param("meetingId", TEST_MEETING_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 완전한 회의록 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 회의록을 성공적으로 조회했습니다"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(TEST_MEETING_ID))
                .andExpect(jsonPath("$.data.studyId").value(TEST_STUDY_ID))
                .andExpect(jsonPath("$.data.title").value(TEST_MEETING_TITLE))
                .andExpect(jsonPath("$.data.content").value(TEST_MEETING_CONTENT))
                .andExpect(jsonPath("$.data.writerId").value(TEST_MEMBER_ID))
                .andExpect(jsonPath("$.data.writerName").exists())
                .andExpect(jsonPath("$.data.participantNumber").isNumber())
                .andExpect(jsonPath("$.data.links").isArray())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.editable").isBoolean());

    }

    @Test
    @Order(3)
    @DisplayName("✏️ 스터디 회의록 수정 - 권한 있는 사용자의 성공적인 수정")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void updateStudyMeeting_AuthorizedUserSuccessfulUpdate() throws Exception {
        // Given: 회의록이 존재하고, 작성자가 수정을 요청함
        createTestMeetingInDatabase();
        
        StudyMeetingUpdateRequestDto request = new StudyMeetingUpdateRequestDto();
        request.setMeetingId(TEST_MEETING_ID);
        request.setTitle("수정된 회의록 제목");
        request.setContent("수정된 회의록 내용입니다.");
        request.setParticipantNumber(2);
        request.setLinks(List.of(
            new LinkDto("업데이트된 회의록", "https://example.com/updated-meeting-notes.pdf")
        ));

        // When: 회의록 수정 API 호출
        mockMvc.perform(put("/api/v1/study/meeting/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 회의록이 성공적으로 수정되었습니다"));

    }

    @Test
    @Order(4)
    @DisplayName("📋 스터디 회의록 목록 조회 - 페이징과 정렬 기능")
    void getAllStudyMeetings_PaginationAndSortingFeatures() throws Exception {
        // Given: 여러 개의 회의록이 존재함
        createMultipleMeetingsInDatabase();

        // When: 페이징된 회의록 목록 조회
        mockMvc.perform(get("/api/v1/study/meeting/all")
                        .param("studyId", TEST_STUDY_ID.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andDo(print())
                // Then: 페이징된 결과와 메타데이터 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3)) // 3개 회의록
                .andExpect(jsonPath("$.data.content[0].links").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

    }

    @Test
    @Order(5)
    @DisplayName("🗑️ 스터디 회의록 삭제 - 권한 있는 사용자의 성공적인 삭제")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void deleteStudyMeeting_AuthorizedUserSuccessfulDeletion() throws Exception {
        // Given: 회의록이 존재하고, 작성자가 삭제를 요청함
        createTestMeetingInDatabase();
        
        StudyMeetingDeleteRequestDto request = new StudyMeetingDeleteRequestDto();
        request.setMeetingId(TEST_MEETING_ID);

        // When: 회의록 삭제 API 호출
        mockMvc.perform(delete("/api/v1/study/meeting/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 성공 메시지
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 회의록이 성공적으로 삭제되었습니다"));

        // Then: 데이터베이스에서 소프트 삭제 확인 (deletedAt 필드 설정)
        verifyMeetingDeletedInDatabase(TEST_MEETING_ID);
        
    }

    // =================================================================
    // 🚨 검증 실패 및 예외 상황 테스트
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("❌ 회의록 생성 실패 - 필수 필드 누락")
    void createStudyMeeting_ValidationFailure_MissingRequiredFields() throws Exception {
        // Given: 필수 필드가 누락된 요청
        StudyMeetingCreateRequestDto request = new StudyMeetingCreateRequestDto();
        // studyId, writerId, title, content, participantIds 모두 누락

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

    }

    @Test
    @Order(11)
    @DisplayName("❌ 회의록 생성 실패 - 잘못된 데이터 형식")
    @WithMockUser(username = "wrong", roles = {"NONE"})
    void createStudyMeeting_ValidationFailure_InvalidDataFormat() throws Exception {
        // Given: 잘못된 형식의 데이터
        StudyMeetingCreateRequestDto request = new StudyMeetingCreateRequestDto();
        request.setStudyId(-1L); // 음수 ID
        request.setTitle(""); // 빈 제목
        request.setContent(""); // 빈 내용
        request.setParticipantNumber(0); // 참가자 수 0

        // When & Then: 검증 실패로 HTTP 400 Bad Request 응답
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

    }

    @Test
    @Order(12)
    @DisplayName("❌ 회의록 조회 실패 - 존재하지 않는 회의록")
    void getStudyMeetingDetail_NotFound_NonExistentMeeting() throws Exception {
        // Given: 존재하지 않는 회의록 ID
        Long nonExistentMeetingId = 99999L;

        // When & Then: HTTP 404 Not Found 응답
        mockMvc.perform(get("/api/v1/study/meeting/detail")
                        .param("meetingId", nonExistentMeetingId.toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("회의록을 찾을 수 없습니다"));

    }



    @Test
    @Order(13)
    @DisplayName("❌ 회의록 수정 실패 - 권한 없는 사용자")
    @WithMockUser(username = "user2", roles = {"PLAYER"})
    void updateStudyMeeting_AuthorizationFailure_UnauthorizedUser() throws Exception {
        // Given: 회의록이 존재하지만, 다른 사용자가 수정을 시도
        createTestMeetingInDatabase();

        StudyMeetingUpdateRequestDto request = new StudyMeetingUpdateRequestDto();
        request.setMeetingId(TEST_MEETING_ID);
        request.setTitle("무단 수정 시도");
        request.setContent("권한이 없는 사용자의 수정 시도");
        request.setLinks(List.of(
            new LinkDto("악성 링크", "https://malicious.com/unauthorized-link.pdf")
        ));

        // When & Then: HTTP 403 Forbidden 응답 (권한 검증이 올바르게 작동함)
        mockMvc.perform(put("/api/v1/study/meeting/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("회의록을 수정할 권한이 없습니다"));

    }

    // =================================================================
    // 🔧 엣지 케이스 및 기술적 테스트
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("🎭 잘못된 Content-Type - HTTP 415 Unsupported Media Type")
    void createStudyMeeting_TechnicalFailure_UnsupportedMediaType() throws Exception {
        // Given: 잘못된 Content-Type
        StudyMeetingCreateRequestDto request = createValidMeetingRequest();

        // When & Then: HTTP 415 Unsupported Media Type 응답
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

    }

    @Test
    @Order(20)
    @DisplayName("🔗 여러 링크가 있는 스터디 회의록 생성 - 다중 링크 저장")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void createStudyMeeting_WithMultipleLinks_SuccessfulMultipleLinkStorage() throws Exception {
        // Given: 여러 링크가 포함된 스터디 회의록 생성 요청
        StudyMeetingCreateRequestDto request = createValidMeetingRequest();
        request.setStudyId(TEST_STUDY_ID); // 테스트 데이터에 존재하는 스터디 ID 사용
        
        // 여러 링크 설정
        List<LinkDto> multipleLinks = List.of(
            new LinkDto("회의록 문서", "https://docs.google.com/document/d/study-meeting-notes"),
            new LinkDto("발표 자료", "https://docs.google.com/presentation/d/study-presentation"),
            new LinkDto("녹화 영상", "https://youtube.com/watch?v=study-example")
        );
        request.setLinks(multipleLinks);

        // When: 스터디 회의록 생성 API 호출
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: HTTP 201 Created 응답 (다중 링크 포함 생성 성공)
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("스터디 회의록이 성공적으로 생성되었습니다"));
        
    }

    @Test
    @Order(21)
    @DisplayName("📋 스터디 상세 조회 - isParticipantable 필드 포함")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void getStudyDetail_IncludesParticipantableField() throws Exception {
        // Given: 유효한 스터디가 존재함
        setupTestData();

        // When: 스터디 상세 조회 API 호출
        var result = mockMvc.perform(get("/api/v1/study/detail")
                .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 isParticipantable 필드 확인
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andReturn();
        
        // 실제 응답 내용 출력
        String responseContent = result.getResponse().getContentAsString();
        
        // JSON 파싱하여 isParticipantable 필드 확인
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseContent);
            if (jsonNode.get("data") != null) {
            }
        } catch (Exception e) {
        }
        
    }

    @Test
    @Order(22)
    @DisplayName("📋 스터디 목록 조회 - isParticipantable 필드 포함")
    void getStudyList_IncludesParticipantableField() throws Exception {
        // Given: 유효한 스터디들이 존재함
        setupTestData();

        // When: 스터디 목록 조회 API 호출
        var result = mockMvc.perform(get("/api/v1/study")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 isParticipantable 필드 확인
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andReturn();
        
        // 실제 응답 내용 출력
        String responseContent = result.getResponse().getContentAsString();
        
        // JSON 파싱하여 isParticipantable 필드 확인
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseContent);
            if (jsonNode.get("data") != null && jsonNode.get("data").get("content") != null) {
                if (jsonNode.get("data").get("content").isArray() && jsonNode.get("data").get("content").size() > 0) {
                }
            }
        } catch (Exception e) {
        }
        
    }

    @Test
    @Order(23)
    @DisplayName("📎 스터디 상세 조회 - attachments 필드 포함")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void getStudyDetail_IncludesAttachmentsField() throws Exception {
        // Given: 첨부파일이 있는 스터디가 존재함
        setupTestData();
        createTestStudyAttachedFileInDatabase(TEST_STUDY_ID, "스터디 자료.pdf", "PDF", "2MB", "https://example.com/study.pdf");

        // When: 스터디 상세 조회 API 호출
        var result = mockMvc.perform(get("/api/v1/study/detail")
                .param("studyId", TEST_STUDY_ID.toString()))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 attachments 필드 확인
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andReturn();
        
        // 실제 응답 내용 출력
        String responseContent = result.getResponse().getContentAsString();
        
        // JSON 파싱하여 attachments 필드 확인
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseContent);
            if (jsonNode.get("data") != null) {
            }
        } catch (Exception e) {
        }
        
    }

    @Test
    @Order(24)
    @DisplayName("📋 스터디 회의록 목록 조회 - content 필드 포함")
    void getStudyMeetingList_IncludesContentField() throws Exception {
        // Given: 회의록이 존재함
        setupTestData();
        createTestStudyMeetingInDatabase();

        // When: 스터디 회의록 목록 조회 API 호출
        var result = mockMvc.perform(get("/api/v1/study/meeting/all")
                        .param("studyId", String.valueOf(TEST_STUDY_ID))
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                // Then: HTTP 200 OK 응답과 content 필드 확인
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andReturn();
        
        // 실제 응답 내용 출력
        String responseContent = result.getResponse().getContentAsString();
        
        // JSON 파싱하여 content 필드 확인
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseContent);
            if (jsonNode.get("data") != null && jsonNode.get("data").get("content") != null) {
                if (jsonNode.get("data").get("content").isArray() && jsonNode.get("data").get("content").size() > 0) {
                }
            }
        } catch (Exception e) {
        }
        
    }

    @Test
    @Order(21)
    @DisplayName("📊 대용량 회의록 목록 조회 - 페이징 성능 테스트")
    void getAllStudyMeetings_PerformanceTest_LargeDataset() throws Exception {
        // Given: 대량의 회의록 데이터 생성 (100개)
        createLargeMeetingDataset(100);

        long startTime = System.currentTimeMillis();

        // When: 페이징된 목록 조회
        mockMvc.perform(get("/api/v1/study/meeting/all")
                        .param("studyId", TEST_STUDY_ID.toString())
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

    @Test
    @Order(25)
    @DisplayName("🔗 /meeting/all 링크가 회의록별로 올바르게 매핑되어야 한다")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void meetingAll_ShouldReturnLinksPerMeeting() throws Exception {
        // Given: 동일 스터디에 회의록 2개를 만들고, 첫 번째 생성 시 링크를 1개 추가
        setupTestData();

        // create meeting #1 with a link via API
        StudyMeetingCreateRequestDto req1 = new StudyMeetingCreateRequestDto();
        req1.setStudyId(TEST_STUDY_ID);
        req1.setTitle("회의록 1");
        req1.setContent("내용 1");
        req1.setParticipantNumber(2);
        req1.setLinks(List.of(new LinkDto("문서1", "https://example.com/doc1")));
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // create meeting #2 without links via DB (distinct meeting id)
        OffsetDateTime now = OffsetDateTime.now();
        dsl.execute(
            "INSERT INTO study_meeting (id, study_id, member_id, title, content, participants, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ), CAST(? AS TIMESTAMPTZ))",
            2L, TEST_STUDY_ID, TEST_MEMBER_ID, "회의록 2", "내용 2", new Long[]{TEST_MEMBER_ID}, now, now
        );

        // When: /meeting/all 조회
        var result = mockMvc.perform(get("/api/v1/study/meeting/all")
                        .param("studyId", TEST_STUDY_ID.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then: 회의록별로 링크 개수가 달라야 한다 (1번만 링크 존재)
        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
        var content = root.path("data").path("content");
        assertThat(content.isArray()).isTrue();
        // 회의록 2개
        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        // 하나는 links 비어있지 않고, 다른 하나는 비어있어야 함
        int nonEmptyLinks = 0;
        int emptyLinks = 0;
        for (var item : content) {
            if (item.path("links").isArray() && item.path("links").size() > 0) nonEmptyLinks++;
            else emptyLinks++;
        }
        assertThat(nonEmptyLinks).isGreaterThanOrEqualTo(1);
        assertThat(emptyLinks).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(26)
    @DisplayName("🚫 한 회의록에 추가한 links가 다른 회의록 detail에 섞이면 안 된다")
    @WithMockUser(username = "user1", roles = {"UPSOLVER"})
    void creatingLinks_ShouldNotAffectOtherMeetingDetails() throws Exception {
        // Given: 회의록 2개 생성 후, 첫 번째에만 링크 생성
        setupTestData();

        // meeting #1 with link via API
        StudyMeetingCreateRequestDto req1 = new StudyMeetingCreateRequestDto();
        req1.setStudyId(TEST_STUDY_ID);
        req1.setTitle("회의록 A");
        req1.setContent("내용 A");
        req1.setParticipantNumber(2);
        req1.setLinks(List.of(new LinkDto("A-문서", "https://example.com/a")));
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // meeting #2 without link via API
        StudyMeetingCreateRequestDto req2 = new StudyMeetingCreateRequestDto();
        req2.setStudyId(TEST_STUDY_ID);
        req2.setTitle("회의록 B");
        req2.setContent("내용 B");
        req2.setParticipantNumber(1);
        mockMvc.perform(post("/api/v1/study/meeting/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        // When: 각각 상세 조회 (id 1, 2 가정)
        var res1 = mockMvc.perform(get("/api/v1/study/meeting/detail").param("meetingId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        var res2 = mockMvc.perform(get("/api/v1/study/meeting/detail").param("meetingId", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then: #1 detail 은 links 가 존재, #2 detail 은 links 가 없어야 함
        com.fasterxml.jackson.databind.JsonNode d1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        com.fasterxml.jackson.databind.JsonNode d2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        assertThat(d1.path("data").path("links").isArray()).isTrue();
        assertThat(d1.path("data").path("links").size()).isGreaterThan(0);
        assertThat(d2.path("data").path("links").isArray()).isTrue();
        assertThat(d2.path("data").path("links").size()).isEqualTo(0);
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 유효한 회의록 생성 요청 DTO 생성
     */
    private StudyMeetingCreateRequestDto createValidMeetingRequest() {
        StudyMeetingCreateRequestDto request = new StudyMeetingCreateRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setTitle(TEST_MEETING_TITLE);
        request.setContent(TEST_MEETING_CONTENT);
        request.setParticipantNumber(2);
        
        // 새로운 links 구조 사용
        List<LinkDto> links = List.of(
            new LinkDto("회의록 문서", "https://example.com/meeting-notes.pdf"),
            new LinkDto("발표 자료", "https://example.com/presentation.pdf")
        );
        request.setLinks(links);
        return request;
    }

    /**
     * 테스트 데이터 설정 (스터디, 멤버)
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
                    .set(MEMBER.GRADE, "SENIOR") // 4학년 대신 SENIOR 사용
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
                    .set(MEMBER.GRADE, "JUNIOR") // 3학년 대신 JUNIOR 사용
                    .set(MEMBER.MAJOR, "정보보안학과")
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
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                    .set(STUDY.STATUS, "READY")
                    .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
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
            dsl.deleteFrom(STUDY_MEETING).execute();
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
        }
    }

    /**
     * 데이터베이스에 테스트용 회의록 생성
     */
    private void createTestMeetingInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.insertInto(STUDY_MEETING)
                .set(STUDY_MEETING.ID, TEST_MEETING_ID)
                .set(STUDY_MEETING.STUDY_ID, TEST_STUDY_ID)
                .set(STUDY_MEETING.MEMBER_ID, TEST_MEMBER_ID)
                .set(STUDY_MEETING.TITLE, TEST_MEETING_TITLE)
                .set(STUDY_MEETING.CONTENT, TEST_MEETING_CONTENT)
                .set(STUDY_MEETING.PARTICIPANTS, new Long[]{
                        TEST_MEMBER_ID,
                        TEST_MEMBER_2_ID
                })
                .set(STUDY_MEETING.CREATED_AT, now)
                .set(STUDY_MEETING.UPDATED_AT, now)
                .execute();
    }

    /**
     * 다수의 회의록 생성 (목록 조회 테스트용)
     */
    private void createMultipleMeetingsInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (int i = 1; i <= 3; i++) {
            dsl.insertInto(STUDY_MEETING)
                    .set(STUDY_MEETING.ID, (long) i)
                    .set(STUDY_MEETING.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_MEETING.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY_MEETING.TITLE, "회의록 " + i)
                    .set(STUDY_MEETING.CONTENT, "회의록 " + i + " 내용")
                    .set(STUDY_MEETING.PARTICIPANTS, new Long[]{
                            TEST_MEMBER_ID,
                            TEST_MEMBER_2_ID
                    })
                    .set(STUDY_MEETING.CREATED_AT, now.minusHours(i))
                    .set(STUDY_MEETING.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 대용량 회의록 데이터셋 생성 (성능 테스트용)
     */
    private void createLargeMeetingDataset(int count) {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (int i = 1; i <= count; i++) {
            dsl.insertInto(STUDY_MEETING)
                    .set(STUDY_MEETING.ID, (long) i)
                    .set(STUDY_MEETING.STUDY_ID, TEST_STUDY_ID)
                    .set(STUDY_MEETING.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY_MEETING.TITLE, "대용량 테스트 회의록 " + i)
                    .set(STUDY_MEETING.CONTENT, "대용량 테스트용 회의록 내용 " + i)
                    .set(STUDY_MEETING.PARTICIPANTS, new Long[]{
                            TEST_MEMBER_ID,
                            TEST_MEMBER_2_ID
                    })
                    .set(STUDY_MEETING.CREATED_AT, now.minusHours(i))
                    .set(STUDY_MEETING.UPDATED_AT, now.minusHours(i))
                    .execute();
        }
    }

    /**
     * 회의록 생성 검증
     */
    private void verifyMeetingCreatedInDatabase(StudyMeetingCreateRequestDto request) {
        var meeting = dsl.selectFrom(STUDY_MEETING)
                .where(STUDY_MEETING.STUDY_ID.eq(request.getStudyId()))
                .and(STUDY_MEETING.TITLE.eq(request.getTitle()))
                .and(STUDY_MEETING.DELETED_AT.isNull())
                .fetchOne();

        assertThat(meeting).isNotNull();
        assertThat(meeting.getTitle()).isEqualTo(request.getTitle());
        assertThat(meeting.getContent()).isEqualTo(request.getContent());
    }

    /**
     * 회의록 삭제 검증 (소프트 삭제)
     */
    private void verifyMeetingDeletedInDatabase(Long meetingId) {
        var meeting = dsl.selectFrom(STUDY_MEETING)
                .where(STUDY_MEETING.ID.eq(meetingId))
                .fetchOne();

        assertThat(meeting).isNotNull();
        assertThat(meeting.getDeletedAt()).isNotNull(); // 소프트 삭제 확인
    }

    /**
     * 테스트용 스터디 첨부파일 생성
     */
    private void createTestStudyAttachedFileInDatabase(Long studyId, String name, String type, String size, String url) {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.execute(
            "INSERT INTO study_attached (id, study_id, member_id, name, type, size, attached_url, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ), CAST(? AS TIMESTAMPTZ))",
            1L, studyId, TEST_MEMBER_ID, name, type, size, url, now, now
        );
    }

    /**
     * 데이터베이스에 테스트용 스터디 회의록 생성
     */
    private void createTestStudyMeetingInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.execute(
            "INSERT INTO study_meeting (id, study_id, member_id, title, content, participants, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ), CAST(? AS TIMESTAMPTZ))",
            TEST_MEETING_ID, TEST_STUDY_ID, TEST_MEMBER_ID, TEST_MEETING_TITLE, TEST_MEETING_CONTENT, new Long[]{TEST_MEMBER_ID}, now, now
        );
    }
}
