package org.certis.studyplatform.blog.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.blog.application.object.command.CreateBlogCommand;
import org.certis.studyplatform.blog.application.object.command.DeleteBlogCommand;
import org.certis.studyplatform.blog.application.object.command.UpdateBlogCommand;
import org.certis.studyplatform.blog.application.object.query.GetAllBlogsQuery;
import org.certis.studyplatform.blog.application.object.query.GetBlogByIdQuery;
import org.certis.studyplatform.blog.application.object.query.SearchBlogsQuery;
import org.certis.studyplatform.blog.domain.repository.BlogCommandRepository;
import org.certis.studyplatform.blog.domain.repository.BlogQueryRepository;
import org.certis.studyplatform.blog.domain.vo.*;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.vo.StudySummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Blog Domain Service
 *
 * Clean Architecture Domain Layer
 * 블로그 비즈니스 로직 처리
 *
 * CQRS 패턴: Command/Query 객체를 받아서 VO를 생성하여 Repository로 전달
 * ✅ Redis 기반 조회수 관리 통합
 * ✅ BlogViewEntity를 통한 조회수 관리
 * ✅ 중복 조회 방지를 위한 2차 캐시 구현
 * ✅ 매일 자정 Redis → RDB 동기화
 * ✅ 참조 제목 조회 로직 구현 (referenceType과 referenceId 활용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogDomainService {

    private final BlogCommandRepository commandRepository;
    private final BlogQueryRepository queryRepository;
    private final MemberDomainService memberDomainService;
    private final StudyDomainService studyDomainService;
    private final ProjectDomainService projectDomainService;
    private final BlogViewDomainService blogViewDomainService;

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 블로그 생성
     * referenceType에 따라 참조 제목을 조회하여 포함
     */
    public BlogVo createBlog(CreateBlogCommand command) {
        log.info("Domain: Creating blog from command - {}", command.title());

        // referenceType에 따라 참조 제목 조회
        String referenceTitle = getReferenceTitle(command.referenceType(), command.referenceId());

        // BlogVo.createNew() 사용 - 조회된 referenceTitle 포함
        BlogVo blogVo = BlogVo.createNew(
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.referenceType(),
                command.referenceId(),
                referenceTitle, // 조회된 참조 제목 설정
                command.creatorId(),
                null, // creatorName은 저장 후 조회 시 설정
                command.isPublic()
        );

        // Command Repository를 통한 저장
        BlogVo savedBlogVo = commandRepository.save(blogVo);

        // Redis 초기 통계 설정
        BlogIdVo blogIdVo = BlogIdVo.of(savedBlogVo.id().value());
        blogViewDomainService.initializeStats(blogIdVo);

        log.info("Domain: Blog created successfully - ID: {} with reference: {} ({})",
                savedBlogVo.id(), referenceTitle, command.referenceType());
        return savedBlogVo;
    }

    /**
     * 블로그 수정
     * referenceType이 변경되었다면 새로운 참조 제목 조회
     */
    public BlogVo updateBlog(UpdateBlogCommand command) {
        log.info("Domain: Updating blog from command - ID: {}", command.id());

        // 기존 블로그 조회
        BlogVo existingBlog = queryRepository.findById(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.BLOG_DOMAIN_NOT_FOUND,
                        "블로그를 찾을 수 없습니다: " + command.id()));

        // 권한 검증
        validateBlogUpdatePermission(command.requesterId(), existingBlog.creatorId());

        // 참조가 변경되었다면 새로운 참조 제목 조회
        String referenceTitle = getReferenceTitle(command.referenceType(), command.referenceId());

        // BlogVo.updateFrom() 사용
        BlogVo updatedBlogVo = BlogVo.updateFrom(
                existingBlog,
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.referenceType(),
                command.referenceId(),
                referenceTitle,
                command.isPublic()
        );

        BlogVo savedBlogVo = commandRepository.save(updatedBlogVo);
        log.info("Domain: Blog updated successfully - ID: {} with reference: {} ({})",
                savedBlogVo.id(), referenceTitle, command.referenceType());
        return savedBlogVo;
    }

    /**
     * 블로그 삭제
     */
    public void deleteBlog(DeleteBlogCommand command) {
        log.info("Domain: Deleting blog from command - ID: {} by requester: {}",
                command.id(), command.requesterId());

        BlogVo existingBlog = queryRepository.findByIdAndDeletedAtIsNull(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.BLOG_DOMAIN_NOT_FOUND,
                        "블로그를 찾을 수 없습니다: " + command.id()));

        validateBlogDeletePermission(command.requesterId(), existingBlog.creatorId());

        commandRepository.deleteById(existingBlog.id().value());

        BlogIdVo blogIdVo = BlogIdVo.of(existingBlog.id().value());
        blogViewDomainService.deleteStats(blogIdVo);

        log.info("Domain: Blog deleted successfully - ID: {}", existingBlog.id());
    }

    // ================================================================
    // QUERY OPERATIONS
    // ================================================================

    /**
     * 블로그 단건 조회
     * referenceType과 referenceId를 통해 참조 제목도 포함하여 반환
     */
    public BlogVo getBlogById(GetBlogByIdQuery query) {
        log.info("Domain: Getting blog from query - ID: {}, viewerId: {}", query.id(), query.viewerId());

        BlogVo blogVo = queryRepository.findBlogDetailById(query.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.BLOG_DOMAIN_NOT_FOUND,
                        "블로그를 찾을 수 없습니다: " + query.id()));

        // 참조 제목 조회 (referenceType과 referenceId 활용)
        String referenceTitle = getReferenceTitle(blogVo.referenceType(), blogVo.referenceId());

        // 조회수 증가 (Redis)
        if (query.viewerId() != null) {
            blogViewDomainService.incrementViewCountInRedis(query.id(), query.viewerId());
        }

        // ViewCount 조회
        BlogIdVo blogIdVo = BlogIdVo.of(query.id());
        Integer currentViewCount = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // 최신 정보가 반영된 BlogVo 생성
        BlogVo updatedBlogVo = BlogVo.of(
                blogVo.id(),
                blogVo.title(),
                blogVo.description(),
                blogVo.content(),
                blogVo.category(),
                blogVo.referenceType(),
                blogVo.referenceId(),
                referenceTitle, // 조회된 참조 제목
                blogVo.creatorId(),
                blogVo.creatorName(),
                currentViewCount,
                blogVo.createdAt(),
                blogVo.isPublic()
        );

        log.info("Domain: Blog found - ID: {}, viewCount: {}, reference: {} ({})",
                blogVo.id(), currentViewCount, referenceTitle, blogVo.referenceType());
        return updatedBlogVo;
    }

    /**
     * 전체 블로그 조회 (페이징)
     * 각 블로그의 참조 제목을 배치로 조회하여 N+1 문제 해결
     */
    public Page<BlogSummaryVo> getAllBlogs(GetAllBlogsQuery query) {
        log.info("Domain: Getting all blogs from query");

        BlogSearchCriteriaVo emptyCriteria = BlogSearchCriteriaVo.empty();
        BlogSearchResultVo result = queryRepository.findBlogs(emptyCriteria, query.pageable());

        List<BlogSummaryVo> enrichedBlogs = enrichBlogSummariesWithAll(result.blogs());

        Page<BlogSummaryVo> blogPage = new PageImpl<>(enrichedBlogs, query.pageable(), result.totalElements());
        log.info("Domain: All blogs retrieved - found {} blogs with references", blogPage.getTotalElements());
        return blogPage;
    }

    /**
     * 복합 검색 조건으로 블로그 검색
     * 각 블로그의 참조 제목을 배치로 조회하여 N+1 문제 해결
     */
    public Page<BlogSummaryVo> searchBlogsByCriteria(SearchBlogsQuery query) {
        log.info("Domain: Searching blogs from query - keyword: {}, category: {}",
                query.keyword(), query.category());

        BlogSearchCriteriaVo criteria = BlogSearchCriteriaVo.of(
                query.keyword(),
                query.category()
        );

        BlogSearchResultVo result = queryRepository.findBlogs(criteria, query.pageable());

        List<BlogSummaryVo> enrichedBlogs = enrichBlogSummariesWithAll(result.blogs());

        Page<BlogSummaryVo> blogPage = new PageImpl<>(enrichedBlogs, query.pageable(), result.totalElements());
        log.info("Domain: Found {} blogs by criteria with references", blogPage.getTotalElements());
        return blogPage;
    }

    /**
     * 특정 멤버가 완료한 Study/Project만 참조 대상으로 조회
     */
    public List<BlogEnableReferenceVo> getBlogReferenceByCompletedMember(Long memberId) {
        log.info("Domain: Getting blog reference list for completed items by member - {}", memberId);

        List<BlogEnableReferenceVo> referenceList = new ArrayList<>();

        try {
            // 완료된 Study 목록 조회
            List<StudySummaryVo> completedStudies = studyDomainService.getCompletedStudiesListByMember(memberId);
            List<BlogEnableReferenceVo> studyReferences = completedStudies.stream()
                    .map(study -> BlogEnableReferenceVo.of(
                            ArticleReferenceType.STUDY,
                            study.id(),
                            study.title()
                    ))
                    .toList();

            // 완료된 Project 목록 조회
            List<ProjectSummaryVo> completedProjects = projectDomainService.getCompletedProjectsListByMember(memberId);
            List<BlogEnableReferenceVo> projectReferences = completedProjects.stream()
                    .map(project -> BlogEnableReferenceVo.of(
                            ArticleReferenceType.PROJECT,
                            project.id(),
                            project.title()
                    ))
                    .toList();

            referenceList.addAll(studyReferences);
            referenceList.addAll(projectReferences);

            // 정렬
            referenceList.sort((a, b) -> {
                int typeComparison = a.referenceType().compareTo(b.referenceType());
                if (typeComparison != 0) {
                    return typeComparison;
                }
                return a.title().compareTo(b.title());
            });

            log.info("Domain: Completed member's blog reference list retrieved - Member: {}, Studies: {}, Projects: {}",
                    memberId, studyReferences.size(), projectReferences.size());

            return referenceList;

        } catch (Exception e) {
            log.error("Domain: Error retrieving completed blog reference list for member: {}", memberId, e);
            return List.of();
        }
    }



    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * referenceType과 referenceId를 통해 참조 제목 조회
     * Study 또는 Project의 제목을 동적으로 조회
     */
    private String getReferenceTitle(ArticleReferenceType referenceType, Long referenceId) {
        if (referenceType == null || referenceId == null) {
            log.debug("Domain: No reference type or ID provided");
            return null;
        }

        try {
            switch (referenceType) {
                case STUDY -> {
                    log.debug("Domain: Getting study title for referenceId: {}", referenceId);
                    StudyVo studyVo = studyDomainService.getStudyById(new GetStudyByIdQuery(referenceId));
                    log.debug("Domain: Found study title: '{}' for ID: {}", studyVo.title(), referenceId);
                    return studyVo.title();
                }
                case PROJECT -> {
                    log.debug("Domain: Getting project title for referenceId: {}", referenceId);
                    ProjectVo projectVo = projectDomainService.getProjectById(new GetProjectByIdQuery(referenceId));
                    log.debug("Domain: Found project title: '{}' for ID: {}", projectVo.title(), referenceId);
                    return projectVo.title();
                }
                default -> {
                    log.warn("Domain: Unsupported reference type: {}", referenceType);
                    return null;
                }
            }
        } catch (DomainException e) {
            // Study나 Project를 찾을 수 없는 경우
            if (e.getStatus() == ExceptionStatus.STUDY_DOMAIN_NOT_FOUND ||
                    e.getStatus() == ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND) {
                log.warn("Domain: Referenced {} not found for ID: {} - {}",
                        referenceType, referenceId, e.getMessage());
                return null; // 참조 대상이 없어도 블로그 조회는 계속 진행
            }
            // 다른 도메인 예외는 그대로 재전파
            log.error("Domain: Domain exception getting reference title - type: {}, id: {}",
                    referenceType, referenceId, e);
            throw e;
        } catch (Exception e) {
            log.error("Domain: Unexpected error getting reference title - type: {}, id: {}",
                    referenceType, referenceId, e);
            return null;
        }
    }

    /**
     * 여러 블로그의 참조 제목을 배치로 조회 (N+1 문제 해결)
     */
    private Map<Long, String> getReferenceTitlesBatch(List<BlogSummaryVo> blogSummaries, ArticleReferenceType type) {
        // 해당 타입의 참조 ID들 수집
        List<Long> referenceIds = blogSummaries.stream()
                .map(blog -> {
                    try {
                        BlogVo blogDetail = queryRepository.findById(blog.id().value()).orElse(null);
                        if (blogDetail != null && blogDetail.referenceType() == type && blogDetail.referenceId() != null) {
                            return blogDetail.referenceId();
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Domain: Error getting reference ID for blog: {}", blog.id().value(), e);
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (referenceIds.isEmpty()) {
            return Map.of();
        }

        // 각 ID별로 제목 조회
        return referenceIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> getReferenceTitle(type, id)
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * BlogSummaryVo에 ViewCount와 참조 정보를 모두 추가
     */
    private List<BlogSummaryVo> enrichBlogSummariesWithAll(List<BlogSummaryVo> blogSummaries) {
        // 1. 배치로 참조 제목들 조회 (N+1 문제 해결)
        Map<Long, String> studyTitles = getReferenceTitlesBatch(blogSummaries, ArticleReferenceType.STUDY);
        Map<Long, String> projectTitles = getReferenceTitlesBatch(blogSummaries, ArticleReferenceType.PROJECT);

        // 2. 각 블로그에 참조 정보만 추가
        return blogSummaries.stream()
                .map(blog -> {
                    try {
                        return enrichBlogSummaryWithReferenceOnly(blog, studyTitles, projectTitles);
                    } catch (Exception e) {
                        log.error("Domain: Error enriching blog summary with reference - ID: {}", blog.id().value(), e);
                        return blog; // 실패해도 원본은 반환
                    }
                })
                .toList();
    }


    /**
     * BlogSummaryVo에 참조 정보 추가 (미리 조회된 제목 맵 활용)
     */
    private BlogSummaryVo enrichBlogSummaryWithReferenceOnly(
            BlogSummaryVo blog,
            Map<Long, String> studyTitles,
            Map<Long, String> projectTitles) {

        try {
            BlogVo blogDetail = queryRepository.findById(blog.id().value()).orElse(null);
            if (blogDetail == null) {
                return blog;
            }

            ArticleReferenceType referenceType = blogDetail.referenceType();
            Long referenceId = blogDetail.referenceId();
            String referenceTitle = null;

            // 미리 조회된 제목 맵에서 찾기
            if (referenceType == ArticleReferenceType.STUDY && referenceId != null) {
                referenceTitle = studyTitles.get(referenceId);
            } else if (referenceType == ArticleReferenceType.PROJECT && referenceId != null) {
                referenceTitle = projectTitles.get(referenceId);
            }

            if (referenceType != null && referenceTitle != null) {
                return BlogSummaryVo.of(
                        blog.id(),
                        blog.title(),
                        blog.description(),
                        blog.category(),
                        blog.createdAt(),
                        blog.updatedAt(),
                        blog.blogCreatorName(),
                        referenceType,
                        referenceTitle,
                        blog.views(), // 기존 views 값 유지
                        null, // studyId
                        null, // projectId
                        null, // studyTitle
                        null  // projectTitle
                );
            }

            return blog;
        } catch (Exception e) {
            log.error("Domain: Error enriching blog with reference - BlogId: {}", blog.id().value(), e);
            return blog;
        }
    }




    // ================================================================
    // PRIVATE VALIDATION METHODS
    // ================================================================

    /**
     * 블로그 수정 권한 검증
     */
    private void validateBlogUpdatePermission(Long requesterId, Long blogCreatorId) {
        log.debug("Domain: Validating blog update permission - requesterId: {}, creatorId: {}",
                requesterId, blogCreatorId);

        if (requesterId.equals(blogCreatorId)) {
            log.debug("Domain: Update permission granted - requester is blog creator");
            return;
        }

        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Update permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                    "블로그를 수정할 권한이 없습니다");
        }

        log.warn("Domain: Update permission denied - requesterId: {}, creatorId: {}", requesterId, blogCreatorId);
        throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                "블로그를 수정할 권한이 없습니다");
    }

    /**
     * 블로그 삭제 권한 검증
     */
    private void validateBlogDeletePermission(Long requesterId, Long blogCreatorId) {
        log.debug("Domain: Validating blog delete permission - requesterId: {}, creatorId: {}",
                requesterId, blogCreatorId);

        if (requesterId.equals(blogCreatorId)) {
            log.debug("Domain: Delete permission granted - requester is blog creator");
            return;
        }

        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Delete permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                    "블로그를 삭제할 권한이 없습니다");
        }

        log.warn("Domain: Delete permission denied - requesterId: {}, creatorId: {}", requesterId, blogCreatorId);
        throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                "블로그를 삭제할 권한이 없습니다");
    }

    /**
     * Admin용 블로그 공개 유무 토글
     */
    public void toggleBlogPublicStatus(Long blogId, Boolean isPublic, Long adminId) {
        log.info("Domain: Toggling blog public status - ID: {} to {} by admin: {}", blogId, isPublic, adminId);

        // Admin 권한 검증
        validateAdminPermission(adminId);

        // 기존 블로그 조회
        BlogVo existingBlog = queryRepository.findById(blogId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.BLOG_DOMAIN_NOT_FOUND,
                        "블로그를 찾을 수 없습니다: " + blogId));

        // 공개 유무 업데이트
        BlogVo updatedBlogVo = BlogVo.updateFrom(
                existingBlog,
                existingBlog.title(),
                existingBlog.description(),
                existingBlog.content(),
                existingBlog.category(),
                existingBlog.referenceType(),
                existingBlog.referenceId(),
                existingBlog.referenceTitle(),
                isPublic
        );

        commandRepository.save(updatedBlogVo);

        log.info("Domain: Blog public status toggled successfully - ID: {} to {}", blogId, isPublic);
    }

    /**
     * 공개 유무에 따른 블로그 조회 (MemberRole 기준 분기)
     */
    public Page<BlogSummaryVo> getBlogsByPublicStatus(Boolean isPublic, Pageable pageable, Long memberId) {
        log.info("Domain: Getting blogs by public status - isPublic: {}, memberId: {}", isPublic, memberId);

        // MemberRole 조회
        MemberVo member = memberDomainService.getMemberVo(new GetMemberByIdQuery(memberId));
        MemberRole memberRole = member.role();

        // Level별 분기 처리
        if (MemberRole.isLevel4OrAbove(memberRole)) {
            // Level 4 이상: 공개/비공개 모두 조회 가능
            return queryRepository.findByPublicStatus(isPublic, pageable);
        } else if (MemberRole.isLevel5(memberRole)) {
            // Level 5: 공개 블로그만 조회 가능
            return queryRepository.findByPublicStatus(true, pageable);
        } else {
            // Level 4 미만: 공개 블로그만 조회 가능
            return queryRepository.findByPublicStatus(true, pageable);
        }
    }

    /**
     * Admin 권한 검증
     */
    private void validateAdminPermission(Long adminId) {
        log.debug("Domain: Validating admin permission - adminId: {}", adminId);

        try {
            MemberVo adminMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(adminId));
            MemberRole adminRole = adminMember.role();

            if (!MemberRole.isStaffOrAbove(adminRole)) {
                log.warn("Domain: Admin permission denied - adminId: {}, role: {}", adminId, adminRole);
                throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                        "관리자 권한이 필요합니다");
            }

            log.debug("Domain: Admin permission granted - adminId: {}, role: {}", adminId, adminRole);
        } catch (DomainException e) {
            log.warn("Domain: Admin not found: {}", adminId);
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_ACCESS_DENIED,
                    "관리자 권한이 필요합니다");
        }
    }
}