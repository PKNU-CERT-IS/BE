package org.certis.studyplatform.blog.domain.repository;

import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.certis.studyplatform.blog.domain.vo.BlogSearchCriteriaVo;
import org.certis.studyplatform.blog.domain.vo.BlogSearchResultVo;
import org.certis.studyplatform.blog.domain.vo.BlogEnableReferenceVo;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Blog Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 jOOQ로 구현
 *
 * ✅ CQRS 패턴 준수:
 * - 모든 조회 관련 메서드 포함
 * - Command 작업 시 필요한 검증용 조회 메서드도 포함
 */
public interface BlogQueryRepository {

    /**
     * 블로그 상세 조회
     *
     * @param blogId 조회할 블로그 ID
     * @return 블로그 상세 정보 (BlogVo)
     */
    Optional<BlogVo> findBlogDetailById(Long blogId);

    /**
     * 블로그 목록 조회 (페이징)
     *
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 블로그 검색 결과
     */
    BlogSearchResultVo findBlogs(BlogSearchCriteriaVo criteria, Pageable pageable);

    /**
     * 작성 가능한 참조 대상 목록 조회 (프로젝트/스터디)
     *
     * @return 참조 가능한 프로젝트/스터디 목록
     */
    List<BlogEnableReferenceVo> findAvailableReferences();

    // ================= Domain Service 지원 메소드 =================

    /**
     * ✅ 블로그 단건 조회 (Domain Service용)
     *
     * @param blogId 블로그 ID
     * @return BlogVo
     */
    Optional<BlogVo> findById(Long blogId);

    /**
     * ✅ 블로그 삭제되지 않은 블로그 조회 (Domain Service용)
     *
     * @param blogId 블로그 ID
     * @return BlogVo
     */
    Optional<BlogVo> findByIdAndDeletedAtIsNull(Long blogId);

    /**
     * ✅ 블로그 제목 존재 여부 확인 (Optional)
     *
     * @param title 블로그 제목
     * @return 존재 여부
     */
    boolean existsByTitle(String title);

    /**
     * ✅ 블로그 제목 중복 확인 (자신 제외) (Optional)
     *
     * @param title 블로그 제목
     * @param blogId 제외할 블로그 ID
     * @return 중복 여부
     */
    boolean existsByTitleAndIdNot(String title, Long blogId);

    /**
     * 공개 유무에 따른 블로그 조회
     *
     * @param isPublic 공개 유무 (null이면 모든 블로그)
     * @param pageable 페이징 정보
     * @return 블로그 목록
     */
    Page<BlogSummaryVo> findByPublicStatus(Boolean isPublic, Pageable pageable);
}