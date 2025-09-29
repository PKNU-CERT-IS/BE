package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogViewQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static org.certis.generated.jooq.Tables.BLOG_VIEW;


@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogViewQueryRepositoryImpl implements BlogViewQueryRepository {
    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

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
