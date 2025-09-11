package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.blog.domain.repository.BlogViewQueryRepository;
import org.certis.studyplatform.blog.domain.vo.*;
import org.certis.studyplatform.blog.infrastructure.mapper.BlogInfrastructureMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.certis.generated.jooq.Tables.*;
import static org.certis.generated.jooq.Tables.BLOG;
import static org.certis.generated.jooq.Tables.MEMBER;
import static org.certis.generated.jooq.Tables.PROJECT;
import static org.certis.generated.jooq.Tables.STUDY;
import static org.jooq.impl.DSL.noCondition;


@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogViewQueryRepositoryImpl implements BlogViewQueryRepository {
    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final BlogInfrastructureMapper mapper;

    @Override
    public Integer getViewCount(BlogIdVo blogIdVo) {
        log.info("jOOQ: Getting latest view count for blog ID - {}", blogIdVo.value());

        var bv = BLOG_VIEW.as("bv");

        Integer viewCount = dsl.select(bv.VIEW_NUMBER)
                .from(bv)
                .where(bv.BLOG_ID.eq(blogIdVo.value()))
                .fetchOne(0, Integer.class);

        int result = viewCount != null ? viewCount : 0;

        log.info("jOOQ: view count retrieved - Blog ID: {}, Count: {}", blogIdVo.value(), result);

        return result;
    }

}
