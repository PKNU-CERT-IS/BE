package org.certis.studyplatform.blog.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.certis.studyplatform.blog.presentation.dto.request.BlogTogglePublicRequestDto;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestRedisMockConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminBlogController 통합 테스트
 *
 * 🎯 테스트 특징:
 * - Admin 전용 API 테스트
 * - 블로그 공개 유무 토글 기능 테스트
 * - 권한 기반 접근 제어 테스트
 * - Redis 기반 조회수 관리 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class, TestRedisMockConfig.class, AdminBlogControllerTest.MockConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("🔐 AdminBlogController 통합 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminBlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private BlogRedisRepository blogRedisRepository;

    // 테스트 상수
    private static final Long TEST_BLOG_ID = 1L;
    private static final Long TEST_ADMIN_ID = 1L;
    private static final Long TEST_MEMBER_ID = 2L;
    private static final Long TEST_STUDY_ID = 1L;
    
    private static final String TEST_BLOG_TITLE = "Admin 테스트용 블로그";
    private static final String TEST_ADMIN_NAME = "AdminUser";
    private static final String TEST_MEMBER_NAME = "TestUser";

    @BeforeEach
    void setUp() {
        // 데이터 충돌 방지: 관련 테이블 초기화
        dsl.execute("TRUNCATE TABLE blog RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE study RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");

        setupTestData();
        setupMockRedisRepository();
        setupAdminAuthentication();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    // =================================================================
    // 🎯 Admin API 테스트
    // =================================================================

    @Test
    @Order(1)
    @DisplayName("🔐 Admin 블로그 공개 유무 토글 - 공개로 변경")
    void toggleBlogPublicStatus_ToPublic() throws Exception {
        // Given: 비공개 블로그가 존재함
        createPrivateBlogInDatabase();

        // When: Admin이 블로그를 공개로 토글
        BlogTogglePublicRequestDto request = new BlogTogglePublicRequestDto();
        request.setBlogId(TEST_BLOG_ID);
        request.setIsPublic(true);

        mockMvc.perform(put("/api/v1/admin/blog/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: 성공 응답 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("블로그 글이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 블로그가 공개로 변경되었는지 확인
        verifyBlogPublicStatusInDatabase(TEST_BLOG_ID, true);

    }

    @Test
    @Order(2)
    @DisplayName("🔐 Admin 블로그 공개 유무 토글 - 비공개로 변경")
    void toggleBlogPublicStatus_ToPrivate() throws Exception {
        // Given: 공개 블로그가 존재함
        createPublicBlogInDatabase();

        // When: Admin이 블로그를 비공개로 토글
        BlogTogglePublicRequestDto request = new BlogTogglePublicRequestDto();
        request.setBlogId(TEST_BLOG_ID);
        request.setIsPublic(false);

        mockMvc.perform(put("/api/v1/admin/blog/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: 성공 응답 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("블로그 글이 성공적으로 갱신되었습니다"));

        // Then: 데이터베이스에서 블로그가 비공개로 변경되었는지 확인
        verifyBlogPublicStatusInDatabase(TEST_BLOG_ID, false);

    }

    @Test
    @Order(3)
    @DisplayName("❌ Admin 블로그 공개 유무 토글 - 존재하지 않는 블로그")
    void toggleBlogPublicStatus_NonExistentBlog() throws Exception {
        // Given: 존재하지 않는 블로그 ID
        Long nonExistentBlogId = 999L;

        // When: 존재하지 않는 블로그의 공개 유무 토글 시도
        BlogTogglePublicRequestDto request = new BlogTogglePublicRequestDto();
        request.setBlogId(nonExistentBlogId);
        request.setIsPublic(true);

        mockMvc.perform(put("/api/v1/admin/blog/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: 404 에러 응답
                .andExpect(status().isNotFound());

    }

    @Test
    @Order(4)
    @DisplayName("❌ Admin 블로그 공개 유무 토글 - 잘못된 요청 데이터")
    void toggleBlogPublicStatus_InvalidRequest() throws Exception {
        // Given: 잘못된 요청 데이터 (blogId가 null)
        BlogTogglePublicRequestDto request = new BlogTogglePublicRequestDto();
        request.setBlogId(null);
        request.setIsPublic(true);

        mockMvc.perform(put("/api/v1/admin/blog/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                // Then: 400 에러 응답
                .andExpect(status().isBadRequest());

    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        BlogRedisRepository blogRedisRepository() {
            return org.mockito.Mockito.mock(BlogRedisRepository.class);
        }
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 테스트 데이터 설정 (Admin, Member, Study, Blog)
     */
    private void setupTestData() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            
            // Admin 멤버 생성
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_ADMIN_ID)
                    .set(MEMBER.NAME, TEST_ADMIN_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "admin@certis.org")
                    .set(MEMBER.ROLE, "ADMIN")
                    .set(MEMBER.BIRTHDAY, now.minusYears(30))
                    .set(MEMBER.GENDER, "MALE")
                    .set(MEMBER.GRADE, "GRADUATED")
                    .set(MEMBER.MAJOR, "컴퓨터공학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 일반 멤버 생성
            dsl.insertInto(MEMBER)
                    .set(MEMBER.ID, TEST_MEMBER_ID)
                    .set(MEMBER.NAME, TEST_MEMBER_NAME)
                    .set(MEMBER.STUDENT_NUMBER, "user@certis.org")
                    .set(MEMBER.ROLE, "PLAYER")
                    .set(MEMBER.BIRTHDAY, now.minusYears(25))
                    .set(MEMBER.GENDER, "FEMALE")
                    .set(MEMBER.GRADE, "SENIOR")
                    .set(MEMBER.MAJOR, "소프트웨어학과")
                    .set(MEMBER.CREATED_AT, now)
                    .set(MEMBER.UPDATED_AT, now)
                    .onDuplicateKeyIgnore()
                    .execute();

            // 스터디 데이터 생성
            dsl.insertInto(STUDY)
                    .set(STUDY.ID, TEST_STUDY_ID)
                    .set(STUDY.TITLE, "Admin 테스트용 스터디")
                    .set(STUDY.DESCRIPTION, "Admin 테스트를 위한 스터디")
                    .set(STUDY.CONTENT, "스터디 상세 내용")
                    .set(STUDY.MEMBER_ID, TEST_MEMBER_ID)
                    .set(STUDY.CATEGORY, "웹 개발")
                    .set(STUDY.SUBCATEGORY, "풀스택")
                    .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
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
     * 공개 블로그 생성
     */
    private void createPublicBlogInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.insertInto(BLOG)
                .set(BLOG.ID, TEST_BLOG_ID)
                .set(BLOG.TITLE, TEST_BLOG_TITLE)
                .set(BLOG.DESCRIPTION, "Admin 테스트용 블로그 설명")
                .set(BLOG.CONTENT, "Admin 테스트용 블로그 내용")
                .set(BLOG.CATEGORY, "웹 개발")
                .set(BLOG.MEMBER_ID, TEST_MEMBER_ID)
                .set(BLOG.STUDY_ID, TEST_STUDY_ID)
                .set(BLOG.IS_PUBLIC, true)
                .set(BLOG.CREATED_AT, now)
                .set(BLOG.UPDATED_AT, now)
                .execute();
    }

    /**
     * 비공개 블로그 생성
     */
    private void createPrivateBlogInDatabase() {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.insertInto(BLOG)
                .set(BLOG.ID, TEST_BLOG_ID)
                .set(BLOG.TITLE, TEST_BLOG_TITLE)
                .set(BLOG.DESCRIPTION, "Admin 테스트용 블로그 설명")
                .set(BLOG.CONTENT, "Admin 테스트용 블로그 내용")
                .set(BLOG.CATEGORY, "웹 개발")
                .set(BLOG.MEMBER_ID, TEST_MEMBER_ID)
                .set(BLOG.STUDY_ID, TEST_STUDY_ID)
                .set(BLOG.IS_PUBLIC, false)
                .set(BLOG.CREATED_AT, now)
                .set(BLOG.UPDATED_AT, now)
                .execute();
    }

    /**
     * Mock Redis Repository 설정
     */
    private void setupMockRedisRepository() {
        // Redis 조회수 조회 Mock
        when(blogRedisRepository.getViewCount(any())).thenReturn(0L);
        
        // Redis 조회수 증가 Mock
        doNothing().when(blogRedisRepository).addView(any(), any());
        
        // Redis 초기화 Mock
        doNothing().when(blogRedisRepository).initializeStats(any());
    }

    /**
     * Admin 인증 설정
     */
    private void setupAdminAuthentication() {
        // Admin 사용자로 인증 설정
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken(TEST_ADMIN_ID, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 데이터베이스에서 블로그 공개 상태 확인
     */
    private void verifyBlogPublicStatusInDatabase(Long blogId, Boolean expectedIsPublic) {
        Boolean actualIsPublic = dsl.select(BLOG.IS_PUBLIC)
                .from(BLOG)
                .where(BLOG.ID.eq(blogId))
                .fetchOne(BLOG.IS_PUBLIC);

        assertThat(actualIsPublic).isEqualTo(expectedIsPublic);
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        try {
            // 외래 키 제약으로 인해 역순으로 삭제
            dsl.deleteFrom(BLOG).execute();
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(MEMBER).execute();
        } catch (Exception e) {
        }
    }
}
