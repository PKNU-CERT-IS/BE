package org.certis.studyplatform.blog.infrastructure.mapper;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.certis.studyplatform.blog.domain.repository.BlogViewQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogEntity;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogViewEntity;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * BlogInfrastructureMapper 단위 테스트
 *
 * 🎯 테스트 특징:
 * - Redis 기반 view_count 조회 테스트
 * - Entity ↔ VO 변환 테스트
 * - jOOQ Record → VO 변환 테스트
 * - 새로운 필드들 (referenceTitle, views, updatedAt, isPublic) 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔄 BlogInfrastructureMapper 단위 테스트")
class BlogInfrastructureMapperTest {

    @Mock
    private BlogRedisRepository blogRedisRepository;

    private BlogInfrastructureMapper mapper;

    // 테스트 상수
    private static final Long TEST_BLOG_ID = 1L;
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_STUDY_ID = 1L;
    private static final String TEST_TITLE = "테스트 블로그";
    private static final String TEST_DESCRIPTION = "테스트 설명";
    private static final String TEST_CONTENT = "테스트 내용";
    private static final String TEST_CATEGORY = "웹 개발";
    private static final String TEST_CREATOR_NAME = "테스트 작성자";
    private static final String TEST_STUDY_TITLE = "테스트 스터디";
    private static final Integer TEST_VIEW_COUNT = 100;
    private static final OffsetDateTime TEST_CREATED_AT = OffsetDateTime.now();
    private static final OffsetDateTime TEST_UPDATED_AT = OffsetDateTime.now().plusHours(1);

    @BeforeEach
    void setUp() {
        mapper = new BlogInfrastructureMapper(blogRedisRepository);
    }

    @Test
    @DisplayName("Entity → VO 변환 - 기본 필드들")
    void toVo_FromEntity_BasicFields() {
        // Given: BlogEntity 생성
        BlogEntity entity = createTestBlogEntity();

        // When: Entity → VO 변환
        var result = mapper.toVo(entity);

        // Then: 기본 필드들이 올바르게 변환되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.id().value()).isEqualTo(TEST_BLOG_ID);
        assertThat(result.title()).isEqualTo(TEST_TITLE);
        assertThat(result.description()).isEqualTo(TEST_DESCRIPTION);
        assertThat(result.content()).isEqualTo(TEST_CONTENT);
        assertThat(result.category()).isEqualTo(TEST_CATEGORY);
        assertThat(result.creatorId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(result.creatorName()).isNull(); // Entity에서는 creatorName이 null
        assertThat(result.createdAt()).isEqualTo(TEST_CREATED_AT);
        assertThat(result.isPublic()).isTrue();
    }

    @Test
    @DisplayName("Entity → VO 변환 - isPublic 필드")
    void toVo_FromEntity_IsPublicField() {
        // Given: isPublic이 false인 BlogEntity
        BlogEntity entity = createTestBlogEntity().toBuilder()
                .isPublic(false)
                .build();

        // When: Entity → VO 변환
        var result = mapper.toVo(entity);

        // Then: isPublic 필드가 올바르게 변환되었는지 확인
        assertThat(result.isPublic()).isFalse();
    }

    @Test
    @DisplayName("jOOQ Record → BlogSummaryVo 변환 - 데이터베이스 view_count 조회")
    void toBlogSummaryVoFromRecord_WithDatabaseViewCount() {
        // Given: jOOQ Record (데이터베이스에서 view_count 포함)
        Record record = createTestBlogRecord();

        // When: Record → BlogSummaryVo 변환
        var result = mapper.toBlogSummaryVoFromRecord(record);

        // Then: 데이터베이스에서 조회한 view_count가 포함되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.id().value()).isEqualTo(TEST_BLOG_ID);
        assertThat(result.title()).isEqualTo(TEST_TITLE);
        assertThat(result.description()).isEqualTo(TEST_DESCRIPTION);
        assertThat(result.category()).isEqualTo(TEST_CATEGORY);
        assertThat(result.createdAt()).isEqualTo(TEST_CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(TEST_UPDATED_AT);
        assertThat(result.blogCreatorName()).isEqualTo(TEST_CREATOR_NAME);
        assertThat(result.referenceType()).isEqualTo(ArticleReferenceType.STUDY);
        assertThat(result.referenceTitle()).isEqualTo(TEST_STUDY_TITLE);
        assertThat(result.views()).isEqualTo(TEST_VIEW_COUNT); // 데이터베이스에서 조회한 값
    }

    @Test
    @DisplayName("jOOQ Record → BlogSummaryVo 변환 - 데이터베이스에 조회수 없을 때 Redis fallback")
    void toBlogSummaryVoFromRecord_DatabaseEmpty_FallbackToRedis() {
        // Given: jOOQ Record (데이터베이스에 view_count 없음)와 Redis Mock 설정
        Record record = createTestBlogRecord();
        record.setValue(BLOG_VIEW.VIEW_NUMBER.as("view_count"), null); // 데이터베이스에 조회수 없음
        when(blogRedisRepository.getViewCount(any(BlogIdVo.class))).thenReturn(75L);

        // When: Record → BlogSummaryVo 변환
        var result = mapper.toBlogSummaryVoFromRecord(record);

        // Then: 데이터베이스에 조회수가 없을 때 Redis에서 조회한 값이 반환되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.views()).isEqualTo(75);
    }

    @Test
    @DisplayName("jOOQ Record → BlogSummaryVo 변환 - referenceType과 referenceTitle")
    void toBlogSummaryVoFromRecord_ReferenceFields() {
        // Given: Study 참조가 있는 jOOQ Record
        Record record = createTestBlogRecord();

        // When: Record → BlogSummaryVo 변환
        var result = mapper.toBlogSummaryVoFromRecord(record);

        // Then: 참조 필드들이 올바르게 변환되었는지 확인
        assertThat(result.referenceType()).isEqualTo(ArticleReferenceType.STUDY);
        assertThat(result.referenceTitle()).isEqualTo(TEST_STUDY_TITLE);
    }

    @Test
    @DisplayName("jOOQ Record → BlogSummaryVo 변환 - Project 참조")
    void toBlogSummaryVoFromRecord_ProjectReference() {
        // Given: Project 참조가 있는 jOOQ Record
        Record record = createTestProjectBlogRecord();

        // When: Record → BlogSummaryVo 변환
        var result = mapper.toBlogSummaryVoFromRecord(record);

        // Then: Project 참조 필드들이 올바르게 변환되었는지 확인
        assertThat(result.referenceType()).isEqualTo(ArticleReferenceType.PROJECT);
        assertThat(result.referenceTitle()).isEqualTo("테스트 프로젝트");
    }

    @Test
    @DisplayName("jOOQ Record → BlogSummaryVo 변환 - 참조 없는 블로그")
    void toBlogSummaryVoFromRecord_NoReference() {
        // Given: 참조가 없는 jOOQ Record
        Record record = createTestBlogRecordWithoutReference();

        // When: Record → BlogSummaryVo 변환
        var result = mapper.toBlogSummaryVoFromRecord(record);

        // Then: 참조 필드들이 null인지 확인
        assertThat(result.referenceType()).isNull();
        assertThat(result.referenceTitle()).isNull();
    }

    @Test
    @DisplayName("BlogViewEntity → BlogVo 변환 - view_count 포함")
    void toBlogVoWithViewCount_FromBlogViewEntity() {
        // Given: BlogViewEntity와 jOOQ Record
        BlogViewEntity viewEntity = createTestBlogViewEntity();
        Record record = createTestBlogRecord();

        // When: BlogViewEntity와 Record → BlogVo 변환
        var result = mapper.toBlogVoWithViewCount(record, TEST_VIEW_COUNT);

        // Then: view_count가 포함된 BlogVo가 생성되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.id().value()).isEqualTo(TEST_BLOG_ID);
        assertThat(result.title()).isEqualTo(TEST_TITLE);
        assertThat(result.viewCount()).isEqualTo(TEST_VIEW_COUNT);
        assertThat(result.isPublic()).isTrue();
    }

    // =================================================================
    // 🛠️ 헬퍼 메서드들
    // =================================================================

    /**
     * 테스트용 BlogEntity 생성
     */
    private BlogEntity createTestBlogEntity() {
        return BlogEntity.builder()
                .id(TEST_BLOG_ID)
                .title(TEST_TITLE)
                .description(TEST_DESCRIPTION)
                .content(TEST_CONTENT)
                .category(TEST_CATEGORY)
                .memberId(TEST_MEMBER_ID)
                .studyId(TEST_STUDY_ID)
                .projectId(null)
                .createdAt(TEST_CREATED_AT)
                .updatedAt(TEST_UPDATED_AT)
                .isPublic(true)
                .build();
    }

    /**
     * 테스트용 BlogViewEntity 생성
     */
    private BlogViewEntity createTestBlogViewEntity() {
        return BlogViewEntity.builder()
                .id(1L)
                .blogId(TEST_BLOG_ID)
                .viewNumber(TEST_VIEW_COUNT)
                .createdAt(TEST_CREATED_AT)
                .build();
    }

    /**
     * 테스트용 jOOQ Record 생성 (Study 참조)
     */
    private Record createTestBlogRecord() {
        var dsl = DSL.using(org.jooq.SQLDialect.POSTGRES);
        var record = dsl.newRecord(BLOG.ID, BLOG.TITLE, BLOG.DESCRIPTION, BLOG.CONTENT, BLOG.CATEGORY,
                BLOG.CREATED_AT, BLOG.UPDATED_AT, BLOG.MEMBER_ID, MEMBER.NAME.as("creator_name"),
                BLOG.STUDY_ID, BLOG.PROJECT_ID, STUDY.TITLE.as("study_title"),
                PROJECT.TITLE.as("project_title"), BLOG.IS_PUBLIC, BLOG_VIEW.VIEW_NUMBER.as("view_count"));
        record.setValue(BLOG.ID, TEST_BLOG_ID);
        record.setValue(BLOG.TITLE, TEST_TITLE);
        record.setValue(BLOG.DESCRIPTION, TEST_DESCRIPTION);
        record.setValue(BLOG.CONTENT, TEST_CONTENT);
        record.setValue(BLOG.CATEGORY, TEST_CATEGORY);
        record.setValue(BLOG.CREATED_AT, TEST_CREATED_AT);
        record.setValue(BLOG.UPDATED_AT, TEST_UPDATED_AT);
        record.setValue(BLOG.MEMBER_ID, TEST_MEMBER_ID);
        record.setValue(MEMBER.NAME.as("creator_name"), TEST_CREATOR_NAME);
        record.setValue(BLOG.STUDY_ID, TEST_STUDY_ID);
        record.setValue(BLOG.PROJECT_ID, null);
        record.setValue(STUDY.TITLE.as("study_title"), TEST_STUDY_TITLE);
        record.setValue(PROJECT.TITLE.as("project_title"), null);
        record.setValue(BLOG.IS_PUBLIC, true);
        record.setValue(BLOG_VIEW.VIEW_NUMBER.as("view_count"), TEST_VIEW_COUNT);
        return record;
    }

    /**
     * 테스트용 jOOQ Record 생성 (Project 참조)
     */
    private Record createTestProjectBlogRecord() {
        var dsl = DSL.using(org.jooq.SQLDialect.POSTGRES);
        var record = dsl.newRecord(BLOG.ID, BLOG.TITLE, BLOG.DESCRIPTION, BLOG.CONTENT, BLOG.CATEGORY,
                BLOG.CREATED_AT, BLOG.UPDATED_AT, BLOG.MEMBER_ID, MEMBER.NAME.as("creator_name"),
                BLOG.STUDY_ID, BLOG.PROJECT_ID, STUDY.TITLE.as("study_title"),
                PROJECT.TITLE.as("project_title"), BLOG.IS_PUBLIC, BLOG_VIEW.VIEW_NUMBER.as("view_count"));
        record.setValue(BLOG.ID, TEST_BLOG_ID);
        record.setValue(BLOG.TITLE, TEST_TITLE);
        record.setValue(BLOG.DESCRIPTION, TEST_DESCRIPTION);
        record.setValue(BLOG.CONTENT, TEST_CONTENT);
        record.setValue(BLOG.CATEGORY, TEST_CATEGORY);
        record.setValue(BLOG.CREATED_AT, TEST_CREATED_AT);
        record.setValue(BLOG.UPDATED_AT, TEST_UPDATED_AT);
        record.setValue(BLOG.MEMBER_ID, TEST_MEMBER_ID);
        record.setValue(MEMBER.NAME.as("creator_name"), TEST_CREATOR_NAME);
        record.setValue(BLOG.STUDY_ID, null);
        record.setValue(BLOG.PROJECT_ID, 1L);
        record.setValue(STUDY.TITLE.as("study_title"), null);
        record.setValue(PROJECT.TITLE.as("project_title"), "테스트 프로젝트");
        record.setValue(BLOG.IS_PUBLIC, true);
        record.setValue(BLOG_VIEW.VIEW_NUMBER.as("view_count"), TEST_VIEW_COUNT);
        return record;
    }

    /**
     * 테스트용 jOOQ Record 생성 (참조 없음)
     */
    private Record createTestBlogRecordWithoutReference() {
        var dsl = DSL.using(org.jooq.SQLDialect.POSTGRES);
        var record = dsl.newRecord(BLOG.ID, BLOG.TITLE, BLOG.DESCRIPTION, BLOG.CONTENT, BLOG.CATEGORY,
                BLOG.CREATED_AT, BLOG.UPDATED_AT, BLOG.MEMBER_ID, MEMBER.NAME.as("creator_name"),
                BLOG.STUDY_ID, BLOG.PROJECT_ID, STUDY.TITLE.as("study_title"),
                PROJECT.TITLE.as("project_title"), BLOG.IS_PUBLIC, BLOG_VIEW.VIEW_NUMBER.as("view_count"));
        record.setValue(BLOG.ID, TEST_BLOG_ID);
        record.setValue(BLOG.TITLE, TEST_TITLE);
        record.setValue(BLOG.DESCRIPTION, TEST_DESCRIPTION);
        record.setValue(BLOG.CONTENT, TEST_CONTENT);
        record.setValue(BLOG.CATEGORY, TEST_CATEGORY);
        record.setValue(BLOG.CREATED_AT, TEST_CREATED_AT);
        record.setValue(BLOG.UPDATED_AT, TEST_UPDATED_AT);
        record.setValue(BLOG.MEMBER_ID, TEST_MEMBER_ID);
        record.setValue(MEMBER.NAME.as("creator_name"), TEST_CREATOR_NAME);
        record.setValue(BLOG.STUDY_ID, null);
        record.setValue(BLOG.PROJECT_ID, null);
        record.setValue(STUDY.TITLE.as("study_title"), null);
        record.setValue(PROJECT.TITLE.as("project_title"), null);
        record.setValue(BLOG.IS_PUBLIC, true);
        record.setValue(BLOG_VIEW.VIEW_NUMBER.as("view_count"), TEST_VIEW_COUNT);
        return record;
    }
}
