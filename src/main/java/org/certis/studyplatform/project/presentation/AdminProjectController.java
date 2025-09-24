package org.certis.studyplatform.project.presentation;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.project.application.ProjectParticipantFacadeService;
import org.certis.studyplatform.project.application.command.ProjectCommandService;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.certis.studyplatform.project.presentation.dto.request.AdminProjectParticipantApprovalRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.AdminProjectParticipantApprovalResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Project Controller
 *
 * 프로젝트 관련 관리자 기능을 제공하는 컨트롤러
 * Staff 이상 권한을 가진 사용자만 접근 가능
 */
@RestController
@RequestMapping("/api/v1/admin/project")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')")
public class AdminProjectController {

    private final ProjectParticipantFacadeService projectParticipantFacadeService;
    private final ProjectCommandService projectCommandService;
    private final ProjectJpaRepository projectJpaRepository;

    /**
     * 프로젝트 참가 신청 승인
     *
     * @param request 승인 요청 DTO
     * @param currentUser 현재 로그인한 관리자
     * @return 승인 결과
     */
    @PostMapping("/participant/approve")
    @Operation(summary = "프로젝트 참가 신청 승인", description = "프로젝트 참가 신청을 승인합니다")
    public ResponseEntity<GlobalResponseHandler<AdminProjectParticipantApprovalResponseDto>> approveProjectParticipant(
            @Valid @RequestBody AdminProjectParticipantApprovalRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("Admin: Approving project participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), currentUser.getId());

        AdminProjectParticipantApprovalResponseDto response = projectParticipantFacadeService.approveParticipantByAdmin(
                request, currentUser.getId());

        log.info("Admin: Project participant approved successfully - participantId: {}", 
                response.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_APPROVE_SUCCESS, response);
    }

    /**
     * 프로젝트 참가 신청 거절
     *
     * @param request 거절 요청 DTO
     * @param currentUser 현재 로그인한 관리자
     * @return 거절 결과
     */
    @PostMapping("/participant/reject")
    @Operation(summary = "프로젝트 참가 신청 거절", description = "프로젝트 참가 신청을 거절합니다")
    public ResponseEntity<GlobalResponseHandler<AdminProjectParticipantApprovalResponseDto>> rejectProjectParticipant(
            @Valid @RequestBody AdminProjectParticipantApprovalRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("Admin: Rejecting project participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), currentUser.getId());

        AdminProjectParticipantApprovalResponseDto response = projectParticipantFacadeService.rejectParticipantByAdmin(
                request, currentUser.getId());

        log.info("Admin: Project participant rejected successfully - participantId: {}", 
                response.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.PROJECT_PARTICIPANT_REJECT_SUCCESS, response);
    }

    @PostMapping("/end/approve")
    @Operation(summary = "프로젝트 종료 제출 승인", description = "프로젝트 종료 제출을 승인하고 endedAt을 설정합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> approveProjectEnd(
            @RequestParam Long projectId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        projectCommandService.approveProjectEnd(projectId, currentUser.getId());
        return GlobalResponseHandler.success(ResponseStatus.PROJECT_END_SUCCESS);
    }

    @PostMapping("/end/reject")
    @Operation(summary = "프로젝트 종료 제출 거절", description = "프로젝트 종료 제출을 거절하고 첨부를 삭제합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> rejectProjectEnd(
            @RequestParam Long projectId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        projectCommandService.rejectProjectEnd(projectId, currentUser.getId());
        return GlobalResponseHandler.success(ResponseStatus.PROJECT_END_REJECT_SUCCESS);
    }

    @GetMapping("/end/{projectId}")
    @Operation(summary = "프로젝트 종료 제출 조회", description = "제출 상태/시간/첨부를 조회합니다")
    public ResponseEntity<GlobalResponseHandler<Map<String, Object>>> getProjectEndSubmission(
            @PathVariable Long projectId) {
        ProjectEntity e = projectJpaRepository.findById(projectId).orElse(null);
        Map<String, Object> body = Map.of(
                "projectId", projectId,
                "status", e != null ? String.valueOf(e.getResultSubmitStatus()) : null,
                "submittedAt", e != null ? e.getResultSubmittedAt() : null,
                "attachments", e != null ? e.getResultAttachmentUrl() : null
        );
        return GlobalResponseHandler.success(ResponseStatus.PROJECT_FIND_SUCCESS, body);
    }

    @PostMapping("/create/approve")
    @Operation(summary = "프로젝트 생성 승인", description = "프로젝트 생성 요청을 승인하고 유예기간을 연장합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> approveProjectCreation(
            @RequestParam Long projectId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        projectCommandService.approveProjectCreation(projectId, currentUser.getId());
        return GlobalResponseHandler.success(ResponseStatus.PROJECT_UPDATE_SUCCESS);
    }

    @PostMapping("/create/reject")
    @Operation(summary = "프로젝트 생성 거절", description = "프로젝트 생성 요청을 거절하고 소프트 삭제합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> rejectProjectCreation(
            @RequestParam Long projectId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        projectCommandService.rejectProjectCreation(projectId, currentUser.getId());
        return GlobalResponseHandler.success(ResponseStatus.PROJECT_DELETE_SUCCESS);
    }
}
