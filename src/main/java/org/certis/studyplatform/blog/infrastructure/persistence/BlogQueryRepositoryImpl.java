package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.domain.vo.BlogSearchCriteriaVo;
import org.certis.studyplatform.blog.domain.vo.BlogSearchResultVo;
import org.certis.studyplatform.blog.domain.vo.BlogEnableReferenceVo;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.blog.infrastructure.mapper.BlogInfrastructureMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.certis.generated.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Blog Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ BlogEntity 구조에 맞는 쿼리 (studyId, projectId 활용)
 * ✅ JOIN을 통한 참조 정보 한번에 조회 (N+1 문제 해결)
 * ✅ ArticleReferenceType 동적 결정 로직
 * ✅ 타입 안전한 jOOQ 쿼리
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogQueryRepositoryImpl implements BlogQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final BlogInfrastructureMapper mapper;

    @Override
    public Optional<BlogVo> findBlogDetailById(Long blogId) {
        log.info("jOOQ: Finding blog detail by ID - {}", blogId);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        Optional<BlogVo> result = dsl.select(
                        b.ID,
                        b.TITLE,
                        b.DESCRIPTION,
                        b.CONTENT,
                        b.CATEGORY,
                        b.STUDY_ID,
                        b.PROJECT_ID,
                        s.TITLE.as("study_title"),
                        p.TITLE.as("project_title"),
                        b.MEMBER_ID,
                        m.NAME.as("creator_name"),
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        b.CREATED_AT,
                        b.UPDATED_AT,
                        b.IS_PUBLIC
                )
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .where(b.ID.eq(blogId))
                .and(b.DELETED_AT.isNull())
                .fetchOptional(mapper::toBlogVoFromRecord);

        if (result.isPresent()) {
            log.info("jOOQ: Blog detail found - ID: {}", blogId);
        } else {
            log.warn("jOOQ: Blog detail not found - ID: {}", blogId);
        }
        return result;
    }

    @Override
    public BlogSearchResultVo findBlogs(BlogSearchCriteriaVo criteria, Pageable pageable) {
        log.info("jOOQ: Finding blogs with criteria - {}", criteria);
        log.debug("Pageable info - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        // 동적 조건 구성
        Condition conditions = buildSearchConditions(criteria);
        log.debug("jOOQ: Search conditions built successfully");

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .where(conditions.and(b.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        log.debug("jOOQ: Count query result: {} total blogs found", total);

        if (total == 0) {
            log.debug("jOOQ: No blogs found matching criteria, returning empty result");
            return BlogSearchResultVo.empty();
        }

        // 페이징된 데이터 조회 (참조 정보 포함)
        var bv = BLOG_VIEW.as("bv");
        List<BlogSummaryVo> blogSummaries = dsl.select(
                        b.ID,
                        b.TITLE,
                        b.DESCRIPTION,
                        b.CATEGORY,
                        b.CREATED_AT,
                        b.UPDATED_AT,
                        m.NAME.as("creator_name"),
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        b.STUDY_ID,
                        b.PROJECT_ID,
                        s.TITLE.as("study_title"),
                        p.TITLE.as("project_title"),
                        b.IS_PUBLIC,
                        bv.VIEW_NUMBER.as("view_count")
                )
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .leftJoin(bv).on(b.ID.eq(bv.BLOG_ID))
                .where(conditions.and(b.DELETED_AT.isNull()))
                .orderBy(buildOrderBy(pageable))
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetchStream()
                .map(mapper::toBlogSummaryVoFromRecord)
                .collect(Collectors.toList());

        log.debug("jOOQ: Data query executed successfully, found {} blog summaries",
                blogSummaries.size());

        log.info("jOOQ: Found {} blogs", total);

        return BlogSearchResultVo.of(blogSummaries, (long) total);
    }

    @Override
    public Optional<BlogVo> findById(Long blogId) {
        log.info("jOOQ: Finding blog VO by ID - {}", blogId);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        Optional<BlogVo> result = dsl.select(
                        b.ID,
                        b.TITLE,
                        b.DESCRIPTION,
                        b.CONTENT,
                        b.CATEGORY,
                        b.STUDY_ID,
                        b.PROJECT_ID,
                        s.TITLE.as("study_title"),
                        p.TITLE.as("project_title"),
                        b.MEMBER_ID,
                        m.NAME.as("creator_name"),
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        b.CREATED_AT,
                        b.UPDATED_AT,
                        b.IS_PUBLIC
                )
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .where(b.ID.eq(blogId))
                .fetchOptional(mapper::toBlogVo);

        if (result.isPresent()) {
            log.info("jOOQ: Blog VO found - ID: {}", blogId);
        } else {
            log.warn("jOOQ: Blog VO not found - ID: {}", blogId);
        }
        return result;
    }

    @Override
    public Optional<BlogVo> findByIdAndDeletedAtIsNull(Long blogId) {
        log.info("jOOQ: Finding blog VO by ID (not deleted) - {}", blogId);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        Optional<BlogVo> result = dsl.select(
                        b.ID,
                        b.TITLE,
                        b.DESCRIPTION,
                        b.CONTENT,
                        b.CATEGORY,
                        b.STUDY_ID,
                        b.PROJECT_ID,
                        s.TITLE.as("study_title"),
                        p.TITLE.as("project_title"),
                        b.MEMBER_ID,
                        m.NAME.as("creator_name"),
                        b.CREATED_AT,
                        b.UPDATED_AT,
                        b.IS_PUBLIC
                )
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .where(b.ID.eq(blogId))
                .and(b.DELETED_AT.isNull())
                .fetchOptional(mapper::toBlogVo);

        if (result.isPresent()) {
            log.info("jOOQ: Blog VO found - ID: {}", blogId);
        } else {
            log.warn("jOOQ: Blog VO not found - ID: {}", blogId);
        }
        return result;
    }

    @Override
    public List<BlogEnableReferenceVo> findAvailableReferences() {
        log.info("jOOQ: Finding available references for blog creation");

        var p = PROJECT.as("p");
        var s = STUDY.as("s");

        // 프로젝트 참조 목록 조회
        List<BlogEnableReferenceVo> projectReferences = dsl.select(
                        p.ID.as("reference_id"),
                        p.TITLE.as("title")
                )
                .from(p)
                .where(p.DELETED_AT.isNull())
                .orderBy(p.CREATED_AT.desc())
                .fetch(record -> BlogEnableReferenceVo.of(
                        ArticleReferenceType.PROJECT,
                        record.get("reference_id", Long.class),
                        record.get("title", String.class)
                ));

        // 스터디 참조 목록 조회
        List<BlogEnableReferenceVo> studyReferences = dsl.select(
                        s.ID.as("reference_id"),
                        s.TITLE.as("title")
                )
                .from(s)
                .where(s.DELETED_AT.isNull())
                .orderBy(s.CREATED_AT.desc())
                .fetch(record -> BlogEnableReferenceVo.of(
                        ArticleReferenceType.STUDY,
                        record.get("reference_id", Long.class),
                        record.get("title", String.class)
                ));

        // 두 목록을 합치고 정렬
        List<BlogEnableReferenceVo> allReferences = new java.util.ArrayList<>();
        allReferences.addAll(projectReferences);
        allReferences.addAll(studyReferences);

        log.info("jOOQ: Found {} project references and {} study references",
                projectReferences.size(), studyReferences.size());
        return allReferences;
    }

    @Override
    public boolean existsByTitle(String title) {
        log.info("jOOQ: Checking if blog exists by title - {}", title);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(BLOG)
                        .where(BLOG.TITLE.eq(title))
                        .and(BLOG.DELETED_AT.isNull())
        );

        log.info("jOOQ: Blog exists by title: {} - title: {}", exists, title);
        return exists;
    }

    @Override
    public boolean existsByTitleAndIdNot(String title, Long blogId) {
        log.info("jOOQ: Checking if blog exists by title excluding ID - title: {}, excludeId: {}",
                title, blogId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(BLOG)
                        .where(BLOG.TITLE.eq(title))
                        .and(BLOG.ID.ne(blogId))
                        .and(BLOG.DELETED_AT.isNull())
        );

        log.info("jOOQ: Blog exists by title (excluding ID): {} - title: {}, excludeId: {}",
                exists, title, blogId);
        return exists;
    }

    @Override
    public Page<BlogSummaryVo> findByPublicStatus(Boolean isPublic, Pageable pageable, BlogSearchCriteriaVo criteria) {
        log.info("jOOQ: Finding blogs by public status - isPublic: {}", isPublic);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        // 조건 구성: 공개 여부 + 검색 조건
        Condition conditions = b.DELETED_AT.isNull();
        if (isPublic != null) {
            conditions = conditions.and(b.IS_PUBLIC.eq(isPublic));
        }
        if (criteria != null) {
            Condition searchConditions = buildSearchConditions(criteria);
            if (searchConditions != null) {
                conditions = conditions.and(searchConditions);
            }
        }

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .where(conditions)
                .fetchOne(0, int.class);

        log.debug("jOOQ: Count query result: {} total blogs found", total);

        if (total == 0) {
            log.debug("jOOQ: No blogs found matching public status, returning empty result");
            return Page.empty();
        }

        // 페이징된 데이터 조회 (참조 정보 포함)
        var bv = BLOG_VIEW.as("bv");
        List<BlogSummaryVo> blogSummaries = dsl.select(
                        b.ID,
                        b.TITLE,
                        b.DESCRIPTION,
                        b.CATEGORY,
                        b.CREATED_AT,
                        b.UPDATED_AT,
                        m.NAME.as("creator_name"),
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        b.STUDY_ID,
                        b.PROJECT_ID,
                        s.TITLE.as("study_title"),
                        p.TITLE.as("project_title"),
                        b.IS_PUBLIC,
                        bv.VIEW_NUMBER.as("view_count")
                )
                .from(b)
                .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                .leftJoin(bv).on(b.ID.eq(bv.BLOG_ID))
                .where(conditions)
                .orderBy(buildOrderBy(pageable))
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetchStream()
                .map(mapper::toBlogSummaryVoFromRecord)
                .collect(Collectors.toList());

        log.debug("jOOQ: Data query executed successfully, found {} blog summaries",
                blogSummaries.size());

        log.info("jOOQ: Found {} blogs by public status", total);

        return new PageImpl<>(blogSummaries, pageable, total);
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * jOOQ 생성 테이블로 검색 조건 구성
     * BlogEntity 구조에 맞게 수정 (STUDY_ID, PROJECT_ID 사용)
     */
    private Condition buildSearchConditions(BlogSearchCriteriaVo criteria) {
        log.debug("jOOQ: Building search conditions with criteria: {}", criteria);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        Condition conditions = noCondition();

        // 키워드 검색 (title, description, content, creatorName, 참조 제목 포함)
        if (criteria.hasKeyword()) {
            String likeKeyword = "%" + criteria.keyword() + "%";
            conditions = conditions.and(
                    b.TITLE.likeIgnoreCase(likeKeyword)
                            .or(b.DESCRIPTION.likeIgnoreCase(likeKeyword))
                            .or(b.CONTENT.likeIgnoreCase(likeKeyword))
                            .or(m.NAME.likeIgnoreCase(likeKeyword))
                            .or(s.TITLE.likeIgnoreCase(likeKeyword))  // 스터디 제목 검색 추가
                            .or(p.TITLE.likeIgnoreCase(likeKeyword))  // 프로젝트 제목 검색 추가
            );
            log.debug("jOOQ: Added keyword condition with reference titles: {}", likeKeyword);
        }

        // 카테고리 필터 (정확히 일치하는 검색)
        if (criteria.hasCategory()) {
            conditions = conditions.and(b.CATEGORY.eq(criteria.category()));
            log.debug("jOOQ: Added category condition: {}", criteria.category());
        }

        log.debug("jOOQ: Final conditions built: {}", conditions);
        return conditions;
    }

    /**
     * jOOQ 생성 테이블로 정렬 조건 구성
     */
    private OrderField<?>[] buildOrderBy(Pageable pageable) {
        var b = BLOG.as("b");

        if (pageable.getSort().isUnsorted()) {
            return new OrderField<?>[]{b.CREATED_AT.desc()};
        }

        return pageable.getSort().stream()
                .map(order -> {
                    Field<?> sortField;
                    switch (order.getProperty()) {
                        case "createdAt":
                            sortField = b.CREATED_AT;
                            break;
                        case "updatedAt":
                            sortField = b.UPDATED_AT;
                            break;
                        case "title":
                            sortField = b.TITLE;
                            break;
                        case "memberId":
                            sortField = b.MEMBER_ID;
                            break;
                        default:
                            sortField = b.CREATED_AT; // 기본값
                    }
                    return order.isAscending() ? sortField.asc() : sortField.desc();
                })
                .toArray(OrderField[]::new);
    }

    @Override
    public Page<BlogSummaryVo> findByMemberId(Long memberId, Pageable pageable) {
        log.info("jOOQ: Finding blogs by member - memberId: {}", memberId);

        var b = BLOG.as("b");
        var m = MEMBER.as("m");
        var s = STUDY.as("s");
        var p = PROJECT.as("p");

        Condition condition = b.MEMBER_ID.eq(memberId).and(b.DELETED_AT.isNull());

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(b)
                .where(condition)
                .fetchOne(0, int.class);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<BlogSummaryVo> blogs;
        
        if (pageable.isUnpaged()) {
            // Pageable이 unpaged인 경우 페이징 없이 조회
            blogs = dsl.select(
                            b.ID,
                            b.TITLE,
                            b.DESCRIPTION,
                            b.CATEGORY,
                            b.STUDY_ID,
                            b.PROJECT_ID,
                            s.TITLE.as("study_title"),
                            p.TITLE.as("project_title"),
                            b.MEMBER_ID,
                            m.NAME.as("creator_name"),
                            m.PROFILE_IMAGE.as("creator_profile_image"),
                            b.CREATED_AT,
                            b.UPDATED_AT,
                            b.IS_PUBLIC,
                            // view_count는 별도 조회 필요하므로 0으로 설정
                            inline(0).as("view_count")
                    )
                    .from(b)
                    .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                    .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                    .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                    .where(condition)
                    .orderBy(b.CREATED_AT.desc())
                    .fetch(mapper::toSummaryVoFromRecord);
        } else {
            // Pageable이 페이징된 경우 limit/offset 적용
            blogs = dsl.select(
                            b.ID,
                            b.TITLE,
                            b.DESCRIPTION,
                            b.CATEGORY,
                            b.STUDY_ID,
                            b.PROJECT_ID,
                            s.TITLE.as("study_title"),
                            p.TITLE.as("project_title"),
                            b.MEMBER_ID,
                            m.NAME.as("creator_name"),
                            m.PROFILE_IMAGE.as("creator_profile_image"),
                            b.CREATED_AT,
                            b.UPDATED_AT,
                            b.IS_PUBLIC,
                            // view_count는 별도 조회 필요하므로 0으로 설정
                            inline(0).as("view_count")
                    )
                    .from(b)
                    .leftJoin(m).on(b.MEMBER_ID.eq(m.ID))
                    .leftJoin(s).on(b.STUDY_ID.eq(s.ID).and(s.DELETED_AT.isNull()))
                    .leftJoin(p).on(b.PROJECT_ID.eq(p.ID).and(p.DELETED_AT.isNull()))
                    .where(condition)
                    .orderBy(b.CREATED_AT.desc())
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .fetch(mapper::toSummaryVoFromRecord);
        }

        log.info("jOOQ: Found {} blogs for member {}", total, memberId);
        return new PageImpl<>(blogs, pageable, total);
    }

    @Override
    public long countByMemberId(Long memberId) {
        log.info("jOOQ: Counting blogs by member - memberId: {}", memberId);

        var b = BLOG.as("b");

        long count = dsl.selectCount()
                .from(b)
                .where(b.MEMBER_ID.eq(memberId).and(b.DELETED_AT.isNull()))
                .fetchOne(0, long.class);

        log.info("jOOQ: Found {} blogs for member {}", count, memberId);
        return count;
    }
}