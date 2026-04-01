package org.certis.studyplatform.project.presentation;

import org.certis.studyplatform.project.presentation.dto.request.ProjectEndRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.ProjectFacadeService;
import org.certis.studyplatform.project.presentation.dto.request.ProjectCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectDeleteRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectAdvancedSearchRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectSummaryResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
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
 * Project REST Controller
 *
 * Clean Architecture Presentation Layer
 * 이미지의 API 명세에 따른 프로젝트 관련 엔드포인트 제공
 *
 * Spring Security 미구축 상태에 맞춰 임시로 leaderId를 파라미터로 받음
 * @ModelAttribute를 사용한 커스텀 DTO 파라미터 바인딩
 *
 * API 명세:
 * - POST /api/v1/project/create - 프로젝트 생성
 * - PUT /api/v1/project/update - 프로젝트 정보 수정
 * - DELETE /api/v1/project/delete - 프로젝트 정보 삭제
 * - GET /api/v1/project/detail - 프로젝트 세부 정보 조회
 * - GET /api/v1/project/search - 프로젝트 검색
 * - GET /api/v1/project/search/keyword - 통합 고급 검색 (5가지 필터 지원)
 * - POST /api/v1/project/meeting/create - 프로젝트 회의록 생성
 * - GET /api/v1/project/meeting/detail - 프로젝트 회의록 상세 조회
 * - PUT /api/v1/project/meeting/edit - 프로젝트 회의록 수정
 * - DELETE /api/v1/project/meeting/delete - 프로젝트 회의록 삭제
 */
@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectFacadeService projectFacadeService;

    /**
     * @param request 프로젝트 생성 요청 DTO (leaderId, creatorName 포함)
     * @return 생성된 프로젝트 정보
     */
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createProject(
            @Valid @RequestBody ProjectCreateRequestDto request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Creating project - {}", request.getTitle());

        // Facade Service 호출
        projectFacadeService.createProject(request, currentUser.getId(), idempotencyKey);

        log.info("REST: Project created successfully");

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_CREATE_SUCCESS);
    }

    /**
     * @param request 프로젝트 수정 요청 DTO (projectId, requesterId 포함)
     * @return 수정된 프로젝트 정보
     */
    @PutMapping("/update")
    public ResponseEntity<GlobalResponseHandler<Void>> updateProject(
            @Valid @RequestBody ProjectUpdateRequestDto request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Updating project - ID: {}", request.getProjectId());

        // Facade Service 호출
        projectFacadeService.updateProject(request, currentUser.getId(), idempotencyKey);

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_UPDATE_SUCCESS);
    }

    /**
     * 프로젝트 정보 삭제 (임시 - Spring Security 미구축 상태)
     *
     * @param request 프로젝트 삭제 요청 DTO (projectId, requesterId 포함)
     * @return 성공 응답
     */
    @DeleteMapping("/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteProject(
            @Valid @RequestBody ProjectDeleteRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Deleting project - ID: {} by requester: {}", request.getProjectId(),  currentUser.getId());

        // Facade Service 호출
        projectFacadeService.deleteProject(request, currentUser.getId());

        log.info("REST: Project deleted successfully - ID: {}", request.getProjectId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_DELETE_SUCCESS);
    }

    /**
     * 프로젝트 세부 정보 조회 (@ModelAttribute 사용)
     *
     * @param request 프로젝트 상세 조회 요청 DTO
     * @return 프로젝트 상세 정보
     */
    @GetMapping("/detail")
    public ResponseEntity<GlobalResponseHandler<ProjectDetailResponseDto>> getProjectDetail(
            @Valid @ModelAttribute ProjectDetailRequestDto request) {
        log.info("REST: Getting project detail - ID: {}", request.getProjectId());

        // Facade Service 호출
        ProjectDetailResponseDto projectDetail = projectFacadeService.getProjectDetail(request);

        log.info("REST: Project detail retrieved successfully - ID: {}", projectDetail.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_FIND_SUCCESS, projectDetail);
    }

    /**
     * 프로젝트 통합 검색 (@ModelAttribute 사용)
     * 5가지 필터 조건을 지원하는 통합 고급 검색
     * - keyword: 제목, 설명, 생성자명 포함 검색
     * - semester: 학기 기간 필터 (예: "2025-01", "2024-02")
     * - category: 카테고리 필터
     * - subcategory: 서브카테고리 필터
     * - status: 프로젝트 상태 필터 (Ready, InProgress, Completed)
     *
     * @param searchRequest 통합 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 프로젝트 목록
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectSummaryResponseDto>>> searchProjectsByKeyword(
            @Valid @ModelAttribute ProjectAdvancedSearchRequestDto searchRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Unified project search - keyword: {}, semester: {}, category: {}, subcategory: {}, projectStatus: {}, page: {}, size: {}",
                searchRequest.getKeyword(), searchRequest.getSemester(), searchRequest.getCategory(),
                searchRequest.getSubcategory(), searchRequest.getProjectStatus(),
                pageable.getPageNumber(), pageable.getPageSize());

        // 통합 고급 검색 Facade Service 호출
        Page<ProjectSummaryResponseDto> result = projectFacadeService.searchProjectsAdvanced(searchRequest, pageable);

        log.info("REST: Unified search completed - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_SEARCH_SUCCESS, result);
    }

    /**
     * 전체 프로젝트 조회
     *
     * @param pageable 페이징 정보
     * @return 전체 프로젝트 목록
     */
    @GetMapping
    public ResponseEntity<GlobalResponseHandler<Page<ProjectSummaryResponseDto>>> getAllProjects(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Getting all projects - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<ProjectSummaryResponseDto> result = projectFacadeService.getAllProjects(pageable);

        log.info("REST: All projects retrieved - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_SEARCH_SUCCESS, result);
    }

    /**
     * 프로젝트 회의록 요약 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @return 회의록 요약 목록
     */
    @GetMapping("/{projectId}/meetings")
    public ResponseEntity<GlobalResponseHandler<List<ProjectMeetingSummaryResponseDto>>> getProjectMeetings(
            @PathVariable Long projectId) {
        log.info("REST: Getting meetings for project - ID: {}", projectId);

        // Facade Service 호출 (VO → DTO 변환 포함)
        List<ProjectMeetingSummaryResponseDto> meetings = projectFacadeService.getProjectMeetings(projectId);

        log.info("REST: Found {} meetings for project - ID: {}", meetings.size(), projectId);

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_FIND_SUCCESS, meetings);
    }

    /**
     * 프로젝트 종료
     * POST /api/v1/project/end
     */
    @PostMapping(value = "/end")
    public ResponseEntity<GlobalResponseHandler<ProjectDetailResponseDto>> endProject(
            @Valid @RequestBody ProjectEndRequestDto requestDto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser) {
        log.info("REST: Ending project - ID: {}, requesterId: {}", requestDto.getProjectId(), currentUser.getId());

        // Facade Service 호출 (VO → DTO 변환 포함)
        ProjectDetailResponseDto endedProject = projectFacadeService.endProject(
                requestDto,
            currentUser.getId(),
            idempotencyKey
        );

        log.info("REST: Project ended successfully - ID: {}", endedProject.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_END_SUCCESS, endedProject);
    }
}
