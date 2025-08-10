//package org.certis.studyplatform.project.domain.repository;
//
//import org.certis.studyplatform.member.domain.model.vo.MemberIdVo;
//import org.certis.studyplatform.project.domain.model.vo.ProjectIdVo;
//import org.springframework.data.domain.Pageable;
//
//import java.util.Optional;
//
///**
// * Project Query Repository Interface
// *
// * CQRS Query 측면의 Repository (Read 작업)
// * Domain Layer의 인터페이스
// * Infrastructure Layer에서 jOOQ로 구현
// */
//public interface ProjectQueryRepository {
//
//    /**
//     * 프로젝트 상세 조회
//     *
//     * @param projectId 조회할 프로젝트 ID
//     * @return 프로젝트 상세 정보
//     */
//    Optional<ProjectDetail> findProjectDetailById(ProjectIdVo projectId);
//
//    /**
//     * 프로젝트 목록 조회 (페이징)
//     *
//     * @param pageable 페이징 정보
//     * @return 프로젝트 요약 목록
//     */
//    ProjectSearchResult findProjects(ProjectSearchCriteria criteria, Pageable pageable);
//
//    /**
//     * 회원이 생성한 프로젝트 목록 조회
//     *
//     * @param creatorId 생성자 ID
//     * @param pageable 페이징 정보
//     * @return 프로젝트 요약 목록
//     */
//    ProjectSearchResult findProjectsByCreatorId(MemberIdVo creatorId, Pageable pageable);
//
//    /**
//     * 카테고리별 프로젝트 목록 조회
//     *
//     * @param category 카테고리
//     * @param pageable 페이징 정보
//     * @return 프로젝트 요약 목록
//     */
//    ProjectSearchResult findProjectsByCategory(String category, Pageable pageable);
//
//    /**
//     * 난이도별 프로젝트 목록 조회
//     *
//     * @param difficulty 난이도
//     * @param pageable 페이징 정보
//     * @return 프로젝트 요약 목록
//     */
//    ProjectSearchResult findProjectsByDifficulty(String difficulty, Pageable pageable);
//
//    /**
//     * 진행 중인 프로젝트 목록 조회
//     *
//     * @param pageable 페이징 정보
//     * @return 프로젝트 요약 목록
//     */
//    ProjectSearchResult findActiveProjects(Pageable pageable);
//
//    /**
//     * ReadModel Classes
//     */
//
//    /**
//     * 프로젝트 상세 정보 ReadModel
//     */
//    record ProjectDetail(
//        ProjectIdVo id,
//        MemberIdVo creatorId,
//        String creatorName,
//        String title,
//        String description,
//        String category,
//        String difficulty,
//        java.util.List<String> requiredSkills,
//        Integer participantLimit,
//        Integer currentParticipants,
//        java.time.OffsetDateTime startDate,
//        java.time.OffsetDateTime endDate,
//        java.time.OffsetDateTime createdAt,
//        java.time.OffsetDateTime updatedAt,
//        String status // "모집중", "진행중", "완료"
//    ) {}
//
//    /**
//     * 프로젝트 요약 정보 ReadModel
//     */
//    record ProjectSummary(
//        ProjectIdVo id,
//        MemberIdVo creatorId,
//        String creatorName,
//        String title,
//        String category,
//        String difficulty,
//        Integer participantLimit,
//        Integer currentParticipants,
//        java.time.OffsetDateTime startDate,
//        java.time.OffsetDateTime endDate,
//        String status
//    ) {}
//
//    /**
//     * 프로젝트 검색 결과 ReadModel
//     */
//    record ProjectSearchResult(
//        java.util.List<ProjectSummary> projects,
//        long totalElements,
//        int totalPages,
//        int currentPage,
//        int pageSize
//    ) {}
//
//    /**
//     * 프로젝트 검색 조건 ReadModel
//     */
//    record ProjectSearchCriteria(
//        String keyword,
//        String category,
//        String difficulty,
//        String status,
//        java.util.List<String> skills
//    ) {}
//}