package org.certis.studyplatform.blog.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.application.command.BlogCommandService;
import org.certis.studyplatform.blog.application.mapper.BlogApplicationCommandMapper;
import org.certis.studyplatform.blog.application.mapper.BlogApplicationDtoMapper;
import org.certis.studyplatform.blog.application.mapper.BlogApplicationQueryMapper;
import org.certis.studyplatform.blog.application.object.command.CreateBlogCommand;
import org.certis.studyplatform.blog.application.object.command.DeleteBlogCommand;
import org.certis.studyplatform.blog.application.object.command.UpdateBlogCommand;
import org.certis.studyplatform.blog.application.object.query.GetAllBlogsQuery;
import org.certis.studyplatform.blog.application.object.query.GetBlogByIdQuery;
import org.certis.studyplatform.blog.application.object.query.SearchBlogsQuery;
import org.certis.studyplatform.blog.application.query.BlogQueryService;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.presentation.dto.request.*;
import org.certis.studyplatform.blog.presentation.dto.response.BlogDetailResponseDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogEnableReferenceResponseDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogSummaryResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Blog Facade Service
 *
 * Clean Architecture Application Layer
 * 통합 진입점 - Command와 Query Service 조합
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogFacadeService {

    private final BlogCommandService blogCommandService;
    private final BlogQueryService blogQueryService;
    private final BlogApplicationCommandMapper commandMapper;
    private final BlogApplicationQueryMapper queryMapper;
    private final BlogApplicationDtoMapper dtoMapper;

    @Qualifier("virtualThreadTaskExecutor")
    private final Executor virtualThreadExecutor;

    // ================================================================
    // COMMAND OPERATIONS - 상태 변경 작업
    // ================================================================

    /**
     * 블로그 생성
     */
    public void createBlog(BlogCreateRequestDto requestDto, Long creatorId) {
        log.info("Facade: Creating blog - {}", requestDto.getTitle());

        // DTO → Command Object 변환
        CreateBlogCommand command = commandMapper.toCreateBlogCommand(requestDto, creatorId);

        // Command Service 호출 (VO 반환)
        BlogVo createdVo = blogCommandService.createBlog(command);

        log.info("Facade: Blog created successfully - ID: {}", createdVo.id());
    }

    /**
     * 블로그 수정
     */
    public void updateBlog(BlogUpdateRequestDto requestDto, Long requesterId) {
        log.info("Facade: Updating blog - ID: {}", requestDto.getBlogId());

        // DTO → Command Object 변환
        UpdateBlogCommand command = commandMapper.toUpdateBlogCommand(requestDto, requesterId);

        // Command Service 호출 (VO 반환)
        BlogVo updatedVo = blogCommandService.updateBlog(command);

        log.info("Facade: Blog updated successfully - ID: {}", updatedVo.id());
    }

    /**
     * 블로그 삭제
     */
    public void deleteBlog(BlogDeleteRequestDto requestDto,Long requesterId) {
        log.info("Facade: Deleting blog - ID: {} by requester: {}", requestDto.getBlogId(), requesterId);

        // DTO → Command Object 변환
        DeleteBlogCommand command = commandMapper.toDeleteBlogCommand(requestDto.getBlogId(),requesterId);

        // Command Service 호출 (void 반환)
        blogCommandService.deleteBlog(command);

        log.info("Facade: Blog deleted successfully - ID: {}", requestDto.getBlogId());
    }

    // ================================================================
    // QUERY OPERATIONS - 조회 작업
    // ================================================================

    /**
     * 블로그 상세 조회 (DTO 기반)
     */
    public BlogDetailResponseDto getBlogDetail(BlogDetailRequestDto requestDto) {
        return getBlogDetail(requestDto, null);
    }

    /**
     * 블로그 상세 조회 (DTO 기반, viewerId 포함)
     */
    public BlogDetailResponseDto getBlogDetail(BlogDetailRequestDto requestDto, Long viewerId) {
        log.info("Facade: Getting blog detail - ID: {}, viewerId: {}", requestDto.getBlogId(), viewerId);

        Long blogId = requestDto.getBlogId();

        // 가상 스레드 Executor 사용
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. 블로그 기본 정보 조회
            CompletableFuture<BlogVo> blogFuture = CompletableFuture
                    .supplyAsync(() -> {
                        GetBlogByIdQuery query = queryMapper.toGetBlogByIdQuery(blogId, viewerId);
                        return blogQueryService.getBlogById(query);
                    }, executor);

            // 2. 최종 결과 반환
            CompletableFuture<BlogDetailResponseDto> resultFuture = blogFuture
                    .thenApply(blogVo -> {
                        // 블로그 VO → DTO 변환
                        return dtoMapper.toBlogDetailResponseDto(blogVo);
                    });

            // 최종 결과 반환
            BlogDetailResponseDto result = resultFuture.join(); // 예외 발생 시 전역 핸들러로 전파됨

            log.info("Facade: Blog detail retrieved successfully - ID: {}", result.getId());

            return result;
        }
    }

    /**
     * 전체 블로그 조회 (ResponseDTO 반환)
     */
    public Page<BlogSummaryResponseDto> getAllBlogs(Pageable pageable) {
        log.info("Facade: Getting all blogs with pagination");

        // DTO → Query Object 변환
        GetAllBlogsQuery query = queryMapper.toGetAllBlogsQuery(pageable);

        // Query Service 호출 (VO 반환)
        Page<BlogSummaryVo> blogs = blogQueryService.getAllBlogs(query);

        // VO → DTO 변환 (FacadeService에서만 수행)
        Page<BlogSummaryResponseDto> responseDto = dtoMapper.toBlogSummaryResponseDtoPage(blogs);

        log.info("Facade: All blogs retrieved - found {} blogs", responseDto.getTotalElements());
        return responseDto;
    }


    /**
     * 통합 고급 검색으로 블로그 검색 (5가지 필터 지원)
     *
     * @param requestDto 고급 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 블로그 목록
     */
    public Page<BlogSummaryResponseDto> searchBlogsAdvanced(
            BlogAdvancedSearchRequestDto requestDto, Pageable pageable) {
        log.info("Facade: Advanced searching blogs - keyword: {}, category: {}",
                requestDto.getKeyword(), requestDto.getCategory());

        // DTO → Query Object 변환 (CPU-bound 작업이므로 비동기 처리 불필요)
        SearchBlogsQuery query = queryMapper.toSearchBlogsQuery(requestDto, pageable);

        // 가상 스레드 Executor를 사용하여 I/O-bound 작업을 비동기로 처리
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Page<BlogSummaryResponseDto>> searchFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // Query Service 호출 (I/O-bound 작업)
                        return blogQueryService.searchBlogs(query);
                    }, executor)
                    .thenApply(blogsVo -> {
                        // VO → DTO 변환 (CPU-bound 작업)
                        return dtoMapper.toBlogSummaryResponseDtoPage(blogsVo);
                    });

            // 비동기 작업 완료 대기 및 결과 반환
            Page<BlogSummaryResponseDto> responseDto = searchFuture.join();

            log.info("Facade: Advanced search completed - found {} blogs", responseDto.getTotalElements());
            return responseDto;
        }
    }

    /**
     * 작성 가능한 project/blog 조회
     */
    public List<BlogEnableReferenceResponseDto> getBlogReference(Long memberId) {
        log.info("Facade: Getting blog reference list");

        // 가상 스레드 Executor를 사용하여 비동기로 처리
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<BlogEnableReferenceResponseDto>> referenceFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // Query Service 호출하여 작성 가능한 블로그/프로젝트 목록 조회
                        return blogQueryService.getBlogReference(memberId);
                    }, executor)
                    .thenApply(blogReferenceVoList -> {
                        // VO → DTO 변환
                        return dtoMapper.toBlogEnableReferenceResponseDtoList(blogReferenceVoList);
                    });

            // 비동기 작업 완료 대기 및 결과 반환
            List<BlogEnableReferenceResponseDto> result = referenceFuture.join();

            log.info("Facade: Blog reference list retrieved - found {} items", result.size());
            return result;
        }
    }

    /**
     * Admin용 블로그 공개 유무 토글
     */
    public void toggleBlogPublicStatus(BlogTogglePublicRequestDto requestDto, Long adminId) {
        log.info("Facade: Toggling blog public status - ID: {} by admin: {}", requestDto.getBlogId(), adminId);

        // Command Service 호출
        blogCommandService.toggleBlogPublicStatus(requestDto.getBlogId(), requestDto.getIsPublic(), adminId);

        log.info("Facade: Blog public status toggled successfully - ID: {}", requestDto.getBlogId());
    }

    /**
     * 공개 유무에 따른 블로그 조회
     */
    public Page<BlogSummaryResponseDto> getBlogsByPublicStatus(Boolean isPublic, Pageable pageable, Long memberId) {
        log.info("Facade: Getting blogs by public status - isPublic: {}", isPublic);

        // Query Service 호출
        Page<BlogSummaryVo> blogs = blogQueryService.getBlogsByPublicStatus(isPublic, pageable, memberId);

        // VO → DTO 변환
        Page<BlogSummaryResponseDto> responseDto = dtoMapper.toBlogSummaryResponseDtoPage(blogs);

        log.info("Facade: Blogs retrieved by public status - found {} blogs", responseDto.getTotalElements());
        return responseDto;
    }

}