package org.certis.studyplatform.project.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.ProjectMeetingFacadeService;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDeleteRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingAllRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Project Meeting REST Controller
 *
 * Clean Architecture Presentation Layer
 * 이미지의 API 명세에 따른 프로젝트 회의록 관련 엔드포인트 제공
 *
 * Spring Security 미구축 상태에 맞춰 임시로 leaderId를 파라미터로 받음
 * @ModelAttribute를 사용한 커스텀 DTO 파라미터 바인딩
 *
 * API 명세:
 * - POST /api/v1/project/meeting/create - 프로젝트 회의록 생성
 * - GET /api/v1/project/meeting/detail - 프로젝트 회의록 상세 조회
 * - PUT /api/v1/project/meeting/edit - 프로젝트 회의록 수정
 * - DELETE /api/v1/project/meeting/delete - 프로젝트 회의록 삭제
 * - GET /api/v1/project/meeting/all - 프로젝트 회의록 전체 목록 조회
 */
@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingController {
    // =================================================================
    // PROJECT MEETING ENDPOINTS (/api/v1/project/meeting/*)
    // =================================================================

    private final ProjectMeetingFacadeService projectMeetingFacadeService;

    /**
     * 프로젝트 회의록 생성
     *
     * @param request 회의록 생성 요청 DTO
     * @return 성공 응답
     */
    @PostMapping("/meeting/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createProjectMeeting(
            @Valid @RequestBody ProjectMeetingCreateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Creating project meeting - projectId: {}, title: {}", request.getProjectId(), request.getTitle());

        // Facade Service 호출
        projectMeetingFacadeService.createProjectMeeting(request,currentUser.getId());

        log.info("REST: Project meeting created successfully");

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_MEETING_CREATE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 상세 조회
     *
     * @param request 회의록 상세 조회 요청 DTO
     * @return 회의록 상세 정보
     */
    @GetMapping("/meeting/detail")
    public ResponseEntity<GlobalResponseHandler<ProjectMeetingDetailResponseDto>> getProjectMeetingDetail(
            @Valid @ModelAttribute ProjectMeetingDetailRequestDto request) {
        log.info("REST: Getting project meeting detail - meetingId: {}", request.getMeetingId());

        // Facade Service 호출
        ProjectMeetingDetailResponseDto response = projectMeetingFacadeService.getProjectMeetingDetail(request);

        log.info("REST: Project meeting detail retrieved successfully - ID: {}", response.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_MEETING_FIND_SUCCESS, response);
    }

    /**
     * 프로젝트 회의록 수정
     *
     * @param request 회의록 수정 요청 DTO
     * @return 성공 응답
     */
    @PutMapping("/meeting/edit")
    public ResponseEntity<GlobalResponseHandler<Void>> updateProjectMeeting(
            @Valid @RequestBody ProjectMeetingUpdateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Updating project meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(),currentUser.getId());

        // Facade Service 호출
        projectMeetingFacadeService.updateProjectMeeting(request,currentUser.getId());

        log.info("REST: Project meeting updated successfully");

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_MEETING_UPDATE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 삭제
     *
     * @param request 회의록 삭제 요청 DTO
     * @return 성공 응답
     */
    @DeleteMapping("/meeting/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteProjectMeeting(
            @Valid @RequestBody ProjectMeetingDeleteRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Deleting project meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(), currentUser.getId());

        // Facade Service 호출
        projectMeetingFacadeService.deleteProjectMeeting(request,currentUser.getId());

        log.info("REST: Project meeting deleted successfully");

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_MEETING_DELETE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 전체 목록 조회
     *
     * @param request 회의록 전체 목록 조회 요청 DTO
     * @param pageable 페이징 정보
     * @return 회의록 목록 (페이징)
     */
    @GetMapping("/meeting/all")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectMeetingSummaryResponseDto>>> getAllProjectMeetings(
            @Valid @ModelAttribute ProjectMeetingAllRequestDto request,
            Pageable pageable) {
        log.info("REST: Getting all project meetings - projectId: {}, page: {}, size: {}", 
                request.getProjectId(), pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<ProjectMeetingSummaryResponseDto> response = projectMeetingFacadeService.getAllProjectMeetings(request, pageable);

        log.info("REST: Project meetings retrieved successfully - totalElements: {}, totalPages: {}", 
                response.getTotalElements(), response.getTotalPages());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_MEETING_FIND_SUCCESS, response);
    }
}
