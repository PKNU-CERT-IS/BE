package org.certis.studyplatform.blog.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.application.object.query.GetAllBlogsQuery;
import org.certis.studyplatform.blog.application.object.query.GetBlogByIdQuery;
import org.certis.studyplatform.blog.application.object.query.SearchBlogsQuery;
import org.certis.studyplatform.blog.domain.service.BlogDomainService;
import org.certis.studyplatform.blog.domain.vo.BlogEnableReferenceVo;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Blog Query Service
 *
 * Clean Architecture Application Layer
 * 블로그 읽기 작업 처리 (CQRS Query Side)
 *
 * Query 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogQueryService {

    private final BlogDomainService blogDomainService;

    /**
     * 블로그 상세 조회
     */
    @Transactional(readOnly = true)
    public BlogVo getBlogById(GetBlogByIdQuery query) {
        log.info("Query: Getting blog by ID - {}", query.id());

        // Query 객체를 Domain Service로 전달
        BlogVo blogVo = blogDomainService.getBlogById(query);

        log.info("Query: Blog retrieved successfully - ID: {}", blogVo.id());
        return blogVo;
    }

    /**
     * 전체 블로그 조회
     */
    @Transactional(readOnly = true)
    public Page<BlogSummaryVo> getAllBlogs(GetAllBlogsQuery query) {
        log.info("Query: Getting all blogs with pagination");

        // Query 객체를 Domain Service로 전달
        Page<BlogSummaryVo> blogs = blogDomainService.getAllBlogs(query);

        log.info("Query: All blogs retrieved - found {} blogs", blogs.getTotalElements());
        return blogs;
    }

    /**
     * 블로그 검색
     */
    @Transactional(readOnly = true)
    public Page<BlogSummaryVo> searchBlogs(SearchBlogsQuery query) {
        log.info("Query: Searching blogs - keyword: {}, category: {}",
                query.keyword(), query.category());

        // Query 객체를 Domain Service로 전달
        Page<BlogSummaryVo> blogs = blogDomainService.searchBlogsByCriteria(query);

        log.info("Query: Blog search completed - found {} blogs", blogs.getTotalElements());
        return blogs;
    }

    /**
     * 작성 가능한 project/blog 조회
     */
    @Transactional(readOnly = true)
    public List<BlogEnableReferenceVo> getBlogReference(Long memberId) {
        log.info("Query: Getting blog reference list");

        // Domain Service로 전달
        List<BlogEnableReferenceVo> blogReferenceList = blogDomainService.getBlogReferenceByCompletedMember(memberId);

        log.info("Query: Blog reference list retrieved - found {} items", blogReferenceList.size());
        return blogReferenceList;
    }

    /**
     * 공개 유무에 따른 블로그 조회
     */
    @Transactional(readOnly = true)
    public Page<BlogSummaryVo> getBlogsByPublicStatus(Boolean isPublic, Pageable pageable, Long memberId) {
        log.info("Query: Getting blogs by public status - isPublic: {}", isPublic);

        // Domain Service로 전달
        Page<BlogSummaryVo> blogs = blogDomainService.getBlogsByPublicStatus(isPublic, pageable, memberId);

        log.info("Query: Blogs retrieved by public status - found {} blogs", blogs.getTotalElements());
        return blogs;
    }
}