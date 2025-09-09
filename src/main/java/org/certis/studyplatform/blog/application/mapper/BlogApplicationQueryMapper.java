package org.certis.studyplatform.blog.application.mapper;

import org.certis.studyplatform.blog.application.object.query.GetAllBlogsQuery;
import org.certis.studyplatform.blog.application.object.query.GetBlogByIdQuery;
import org.certis.studyplatform.blog.application.object.query.SearchBlogsQuery;
import org.certis.studyplatform.blog.presentation.dto.request.BlogAdvancedSearchRequestDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Blog Application Query Mapper
 *
 * Presentation Layer 파라미터를 Application Layer Query 객체로 변환
 */
@Component
public class BlogApplicationQueryMapper {

    /**
     * ID를 GetBlogByIdQuery로 변환
     */
    public GetBlogByIdQuery toGetBlogByIdQuery(Long blogId) {
        return GetBlogByIdQuery.ofWithoutViewer(blogId);
    }

    /**
     * Pageable을 GetAllBlogsQuery로 변환
     */
    public GetAllBlogsQuery toGetAllBlogsQuery(Pageable pageable) {
        return GetAllBlogsQuery.of(pageable);
    }

    /**
     * BlogAdvancedSearchRequestDto를 SearchBlogsQuery로 변환 (통합 고급 검색)
     */
    public SearchBlogsQuery toSearchBlogsQuery(BlogAdvancedSearchRequestDto dto, Pageable pageable) {
        return SearchBlogsQuery.ofAdvanced(
                dto.getKeyword(),
                dto.getCategory(),
                pageable
        );
    }
}