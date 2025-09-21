package org.certis.studyplatform.blog.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.application.BlogFacadeService;
import org.certis.studyplatform.blog.presentation.dto.request.BlogCreateRequestDto;
import org.certis.studyplatform.blog.presentation.dto.request.BlogDeleteRequestDto;
import org.certis.studyplatform.blog.presentation.dto.request.BlogDetailRequestDto;
import org.certis.studyplatform.blog.presentation.dto.request.BlogUpdateRequestDto;
import org.certis.studyplatform.blog.presentation.dto.request.BlogAdvancedSearchRequestDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogDetailResponseDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogEnableReferenceResponseDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogSummaryResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Blog REST Controller
 *
 * Clean Architecture Presentation Layer
 * 이미지의 API 명세에 따른 블로그 관련 엔드포인트 제공
 *
 * Spring Security 미구축 상태에 맞춰 임시로 authorId를 파라미터로 받음
 * @ModelAttribute를 사용한 커스텀 DTO 파라미터 바인딩
 *
 * API 명세:
 * - POST /api/v1/blog/create - 블로그 생성
 * - PUT /api/v1/blog/update - 블로그 정보 수정
 * - DELETE /api/v1/blog/delete - 블로그 정보 삭제
 * - GET /api/v1/blog/detail - 블로그 세부 정보 조회
 * - GET /api/v1/blog/search - 블로그 검색
 * - GET /api/v1/blog/search/keyword - 통합 고급 검색 (5가지 필터 지원)
 * - GET /api/v1/blog/blog/reference - 작성 가능한 project/blog 조회
 */
@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
@Slf4j
public class BlogController {

    private final BlogFacadeService blogFacadeService;

    /**
     * 블로그 생성
     *
     * @param request 블로그 생성 요청 DTO (authorId, creatorName 포함)
     * @return 생성된 블로그 정보
     */
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createBlog(
            @Valid @RequestBody BlogCreateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Creating blog - {}", request.getTitle());

        // Facade Service 호출
        blogFacadeService.createBlog(request, currentUser.getId());

        log.info("REST: Blog created successfully");

        return GlobalResponseHandler.success(ResponseStatus.BLOG_CREATE_SUCCESS);
    }

    /**
     * 블로그 정보 수정
     *
     * @param request 블로그 수정 요청 DTO (blogId, requesterId 포함)
     * @return 수정된 블로그 정보
     */
    @PutMapping("/update")
    public ResponseEntity<GlobalResponseHandler<Void>> updateBlog(
            @Valid @RequestBody BlogUpdateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {
        log.info("REST: Updating blog - ID: {}", request.getBlogId());

        // Facade Service 호출
        blogFacadeService.updateBlog(request,currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_UPDATE_SUCCESS);
    }

    /**
     * 블로그 정보 삭제
     *
     * @param request 블로그 삭제 요청 DTO (blogId, requesterId 포함)
     * @return 성공 응답
     */
    @DeleteMapping("/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteBlog(
            @Valid @RequestBody BlogDeleteRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Deleting blog - ID: {} by requester: {}", request.getBlogId(), currentUser.getId());

        // Facade Service 호출
        blogFacadeService.deleteBlog(request,currentUser.getId());

        log.info("REST: Blog deleted successfully - ID: {}", request.getBlogId());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_DELETE_SUCCESS);
    }

    /**
     * 블로그 세부 정보 조회 (@ModelAttribute 사용)
     *
     * @param request 블로그 상세 조회 요청 DTO
     * @param currentUser 현재 사용자 정보
     * @return 블로그 상세 정보
     */
    @GetMapping("/detail")
    public ResponseEntity<GlobalResponseHandler<BlogDetailResponseDto>> getBlogDetail(
            @Valid @ModelAttribute BlogDetailRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        log.info("REST: Getting blog detail - ID: {}, viewerId: {}", request.getBlogId(), currentUser.getId());

        // Facade Service 호출 (viewerId 포함)
        BlogDetailResponseDto blogDetail = blogFacadeService.getBlogDetail(request, currentUser.getId());

        log.info("REST: Blog detail retrieved successfully - ID: {}", blogDetail.getId());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_FIND_SUCCESS, blogDetail);
    }

    /**
     * 블로그 통합 검색 (@ModelAttribute 사용)
     * 5가지 필터 조건을 지원하는 통합 고급 검색
     * - keyword: 제목, 설명, 생성자명 포함 검색
     * - semester: 학기 기간 필터 (예: "2025-01", "2024-02")
     * - category: 카테고리 필터
     * - subcategory: 서브카테고리 필터
     * - status: 블로그 상태 필터 (Ready, InProgress, Completed)
     *
     * @param searchRequest 통합 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 블로그 목록
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<Page<BlogSummaryResponseDto>>> searchBlogsByKeyword(
            @Valid @ModelAttribute BlogAdvancedSearchRequestDto searchRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Unified blog search - keyword: {}, category: {}, page: {}, size: {}",
                searchRequest.getKeyword(), searchRequest.getCategory(),
                pageable.getPageNumber(), pageable.getPageSize());

        // 통합 고급 검색 Facade Service 호출
        Page<BlogSummaryResponseDto> result = blogFacadeService.searchBlogsAdvanced(searchRequest, pageable);

        log.info("REST: Unified search completed - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_SEARCH_SUCCESS, result);
    }

    /**
     * 전체 블로그 조회
     *
     * @param pageable 페이징 정보
     * @return 전체 블로그 목록
     */
    @GetMapping
    public ResponseEntity<GlobalResponseHandler<Page<BlogSummaryResponseDto>>> getAllBlogs(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Getting all blogs - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<BlogSummaryResponseDto> result = blogFacadeService.getAllBlogs(pageable);

        log.info("REST: All blogs retrieved - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_SEARCH_SUCCESS, result);
    }

    /**
     * 작성 가능한 project/blog 조회
     *
     * @return 작성 가능한 블로그 목록
     */
    @GetMapping("/blog/reference")
    public ResponseEntity<GlobalResponseHandler<List<BlogEnableReferenceResponseDto>>> getBlogReference() {
        log.info("REST: Getting blog reference list");

        //TODO: memberID 하드코딩 수정

        Long memberId = 1L;

        // Facade Service 호출
        List<BlogEnableReferenceResponseDto> result = blogFacadeService.getBlogReference(memberId);

        log.info("REST: Blog reference list retrieved - found {} items", result.size());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_FIND_SUCCESS, result);
    }

    /**
     * 공개 유무에 따른 블로그 조회 API
     * GET /api/v1/blog/public
     */
    @GetMapping("/public")
    public ResponseEntity<GlobalResponseHandler<Page<BlogSummaryResponseDto>>> getPublicBlogs(
            @RequestParam(required = false) Boolean isPublic,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Getting public blogs - isPublic: {}, page: {}, size: {}",
                isPublic, pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<BlogSummaryResponseDto> result = blogFacadeService.getBlogsByPublicStatus(isPublic, pageable, currentUser.getId());

        log.info("REST: Public blogs retrieved - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_SEARCH_SUCCESS, result);
    }

}