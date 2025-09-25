package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchCriteriaVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchResultVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Project Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 jOOQ로 구현
 *
 * ✅ CQRS 패턴 준수:
 * - 모든 조회 관련 메서드 포함
 * - Command 작업 시 필요한 검증용 조회 메서드도 포함
 */
public interface ProjectQueryRepository {

    /**
     * 프로젝트 상세 조회
     *
     * @param projectId 조회할 프로젝트 ID
     * @return 프로젝트 상세 정보 (ProjectVo)
     */
    Optional<ProjectVo> findProjectDetailById(Long projectId);

    /**
     * 프로젝트 목록 조회 (페이징)
     *
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findProjects(ProjectSearchCriteriaVo criteria, Pageable pageable);

    /**
     * 회원이 생성한 프로젝트 목록 조회
     *
     * @param memberId 생성자 ID
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findProjectsByMemberId(Long memberId, Pageable pageable);

    /**
     * 카테고리별 프로젝트 목록 조회
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findProjectsByCategory(String category, Pageable pageable);

    /**
     * 진행 중인 프로젝트 목록 조회
     *
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findActiveProjects(Pageable pageable);

    /**
     * 키워드로 프로젝트 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findProjectsByKeyword(String keyword, Pageable pageable);

    /**
     * 스킬로 프로젝트 검색
     *
     * @param skills 스킬 목록
     * @param pageable 페이징 정보
     * @return 프로젝트 검색 결과
     */
    ProjectSearchResultVo findProjectsBySkills(List<String> skills, Pageable pageable);

    // ================= Domain Service 지원 메소드 =================

    /**
     * ✅ 프로젝트 단건 조회 (Domain Service용)
     *
     * @param projectId 프로젝트 ID
     * @return ProjectVo
     */
    Optional<ProjectVo> findById(Long projectId);


    /**
     * ✅ 프로젝트 종료되지 않은 프로젝트 조회 (Domain Service용)
     *
     * @param projectId 프로젝트 ID
     * @return ProjectVo
     */
    Optional<ProjectVo> findByIdAndDeletedAtIsNull(Long projectId);

    /**
     * ✅ 프로젝트 제목 존재 여부 확인
     *
     * @param title 프로젝트 제목
     * @return 존재 여부
     */
    boolean existsByTitle(String title);

    /**
     * ✅ 프로젝트 제목 중복 확인 (자신 제외)
     *
     * @param title 프로젝트 제목
     * @param projectId 제외할 프로젝트 ID
     * @return 중복 여부
     */
    boolean existsByTitleAndIdNot(String title, Long projectId);

    /**
     * 특정 멤버가 생성한 완료된 프로젝트 목록 조회 (페이징)
     * 완료 조건: ended_at < 현재시간 AND deleted_at IS NULL AND member_id = ?
     */
    Page<ProjectSummaryVo> findCompletedProjectsByMember(Long memberId, Pageable pageable);

    /**
     * 특정 멤버가 생성한 완료된 프로젝트 목록 조회 (전체)
     */
    List<ProjectSummaryVo> findCompletedProjectsListByMember(Long memberId);

    java.util.Optional<org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo> getEndSubmissionInfo(Long projectId);

    /**
     * 종료 제출 상태가 INPROGRESS인 프로젝트 목록 조회 (관리자용)
     */
    java.util.List<org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo> findEndSubmissionsInProgress();
}