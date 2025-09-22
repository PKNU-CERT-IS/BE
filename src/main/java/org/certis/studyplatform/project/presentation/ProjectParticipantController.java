package org.certis.studyplatform.project.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.ProjectParticipantFacadeService;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.request.*;
import org.certis.studyplatform.project.presentation.dto.response.*;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Project Participant Controller
 *
 * 프로젝트 참가 관련 REST API 컨트롤러
 * Clean Architecture Presentation Layer
 */
@RestController
@RequestMapping("/api/v1/project/participant")
@RequiredArgsConstructor
@Slf4j
public class ProjectParticipantController {

    private final ProjectParticipantFacadeService projectParticipantFacadeService;

    // ================================================================
    // PROJECT JOIN OPERATIONS - 프로젝트 참가 관리
    // ================================================================

    /**
     * 프로젝트 참가 신청
     *
     * @param requestDto 참가 신청 요청 DTO
     * @return 참가 신청 결과
     */
    @PostMapping("/join/register")
    public ResponseEntity<GlobalResponseHandler<ProjectJoinResponseDto>> registerJoinProject(
            @Valid @RequestBody ProjectJoinRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {

        log.info("Controller: Register project join request - projectId: {}", requestDto.getProjectId());

        ProjectJoinResponseDto responseDto = projectParticipantFacadeService.registerJoinProject(requestDto, currentUser.getId());

        log.info("Controller: Project join registered successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_JOIN_REGISTERED, responseDto);
    }

    /**
     * 프로젝트 참가 신청 취소
     *
     * @param requestDto 참가 신청 취소 요청 DTO
     * @return 취소 결과
     */
    @DeleteMapping("/join/cancel")
    public ResponseEntity<GlobalResponseHandler<Void>> cancelJoinProject(
            @Valid @RequestBody ProjectJoinCancelRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Cancel project join request - projectId: {}", requestDto.getProjectId());

        projectParticipantFacadeService.cancelJoinProject(requestDto, currentUser.getId());

        log.info("Controller: Project join cancelled successfully - projectId: {}",
                requestDto.getProjectId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_JOIN_CANCELED);
    }

    /**
     * 프로젝트 참가 승인
     *
     * @param requestDto 참가 승인 요청 DTO
     * @return 승인 결과
     */
    @PostMapping("/join/approve")
    public ResponseEntity<GlobalResponseHandler<ProjectParticipantStatusUpdateResponseDto>> approveJoinProject(
            @Valid @RequestBody ProjectJoinApproveRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Approve project join request - participantId: {}",
                requestDto.getParticipantId());

        ProjectParticipantStatusUpdateResponseDto responseDto =
                projectParticipantFacadeService.approveJoinProject(requestDto, currentUser.getId());

        log.info("Controller: Project join approved successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_JOIN_APPROVED, responseDto);
    }

    /**
     * 프로젝트 참가 거절
     *
     * @param requestDto 참가 거절 요청 DTO
     * @return 거절 결과
     */
    @PostMapping("/join/reject")
    public ResponseEntity<GlobalResponseHandler<ProjectParticipantStatusUpdateResponseDto>> rejectJoinProject(
            @Valid @RequestBody ProjectJoinRejectRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Reject project join request - participantId: {}",
                requestDto.getParticipantId());

        ProjectParticipantStatusUpdateResponseDto responseDto =
                projectParticipantFacadeService.rejectJoinProject(requestDto, currentUser.getId());

        log.info("Controller: Project join rejected successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_JOIN_REJECTED, responseDto);
    }

    // ================================================================
    // PROJECT PRESENTATION QUERY OPERATIONS - 프로젝트 참가자 조회
    // ================================================================

    /**
     * 프로젝트별 참가자 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @param status 참가자 상태 (선택적)
     * @param pageable 페이징 정보
     * @return 참가자 목록
     */
    @GetMapping("/{projectId}/participants")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectParticipantSummaryResponseDto>>> getProjectParticipants(
            @PathVariable Long projectId,
            @RequestParam(required = false) ProjectParticipantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get project participants - projectId: {}, status: {}", projectId, status);

        Page<ProjectParticipantSummaryResponseDto> participants =
                projectParticipantFacadeService.getProjectParticipants(projectId, status, pageable);

        log.info("Controller: Found {} project participants", participants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_SEARCH_SUCCESS, participants);
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 모든 참가자 목록
     */
    @GetMapping("/{projectId}/participants/all")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectParticipantSummaryResponseDto>>> getAllProjectParticipants(
            @PathVariable Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get all project participants - projectId: {}", projectId);

        Page<ProjectParticipantSummaryResponseDto> participants =
                projectParticipantFacadeService.getAllProjectParticipants(projectId, pageable);

        log.info("Controller: Found {} total project participants", participants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_SEARCH_SUCCESS, participants);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     *
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 참가 프로젝트 목록
     */
    @GetMapping("/members/{memberId}/participations")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectParticipantSummaryResponseDto>>> getMemberParticipations(
            @PathVariable Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get member participations - memberId: {}", memberId);

        Page<ProjectParticipantSummaryResponseDto> participations =
                projectParticipantFacadeService.getMemberParticipations(memberId, pageable);

        log.info("Controller: Found {} member participations", participations.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_SEARCH_SUCCESS, participations);
    }


    // ================================================================
    // CONVENIENCE ENDPOINTS - 편의 기능
    // ================================================================

    /**
     * 프로젝트별 대기 중인 참가 신청 목록 조회 (프로젝트 생성자용)
     *
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 대기 중인 참가 신청 목록
     */
    @GetMapping("/{projectId}/participants/pending")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectParticipantSummaryResponseDto>>> getPendingParticipants(
            @PathVariable Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get pending participants - projectId: {}", projectId);

        Page<ProjectParticipantSummaryResponseDto> pendingParticipants =
                projectParticipantFacadeService.getProjectParticipants(projectId, ProjectParticipantStatus.PENDING, pageable);

        log.info("Controller: Found {} pending participants", pendingParticipants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_SEARCH_SUCCESS, pendingParticipants);
    }

    /**
     * 프로젝트별 승인된 참가자 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 승인된 참가자 목록
     */
    @GetMapping("/{projectId}/participants/approved")
    public ResponseEntity<GlobalResponseHandler<Page<ProjectParticipantSummaryResponseDto>>> getApprovedParticipants(
            @PathVariable Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get approved participants - projectId: {}", projectId);

        Page<ProjectParticipantSummaryResponseDto> approvedParticipants =
                projectParticipantFacadeService.getProjectParticipants(projectId, ProjectParticipantStatus.APPROVED, pageable);

        log.info("Controller: Found {} approved participants", approvedParticipants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_SEARCH_SUCCESS, approvedParticipants);
    }
}