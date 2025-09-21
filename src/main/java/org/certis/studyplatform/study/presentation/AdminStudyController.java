package org.certis.studyplatform.study.presentation;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.study.application.StudyParticipantFacadeService;
import org.certis.studyplatform.study.presentation.dto.request.AdminStudyParticipantApprovalRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.AdminStudyParticipantApprovalResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Study Controller
 *
 * 스터디 관련 관리자 기능을 제공하는 컨트롤러
 * Staff 이상 권한을 가진 사용자만 접근 가능
 */
@RestController
@RequestMapping("/api/v1/admin/study")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')")
public class AdminStudyController {

    private final StudyParticipantFacadeService studyParticipantFacadeService;

    /**
     * 스터디 참가 신청 승인
     *
     * @param request 승인 요청 DTO
     * @param currentUser 현재 로그인한 관리자
     * @return 승인 결과
     */
    @PostMapping("/participant/approve")
    @Operation(summary = "스터디 참가 신청 승인", description = "스터디 참가 신청을 승인합니다")
    public ResponseEntity<GlobalResponseHandler<AdminStudyParticipantApprovalResponseDto>> approveStudyParticipant(
            @Valid @RequestBody AdminStudyParticipantApprovalRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("Admin: Approving study participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), currentUser.getId());

        AdminStudyParticipantApprovalResponseDto response = studyParticipantFacadeService.approveParticipantByAdmin(
                request, currentUser.getId());

        log.info("Admin: Study participant approved successfully - participantId: {}", 
                response.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_APPROVE_SUCCESS, response);
    }

    /**
     * 스터디 참가 신청 거절
     *
     * @param request 거절 요청 DTO
     * @param currentUser 현재 로그인한 관리자
     * @return 거절 결과
     */
    @PostMapping("/participant/reject")
    @Operation(summary = "스터디 참가 신청 거절", description = "스터디 참가 신청을 거절합니다")
    public ResponseEntity<GlobalResponseHandler<AdminStudyParticipantApprovalResponseDto>> rejectStudyParticipant(
            @Valid @RequestBody AdminStudyParticipantApprovalRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("Admin: Rejecting study participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), currentUser.getId());

        AdminStudyParticipantApprovalResponseDto response = studyParticipantFacadeService.rejectParticipantByAdmin(
                request, currentUser.getId());

        log.info("Admin: Study participant rejected successfully - participantId: {}", 
                response.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_REJECT_SUCCESS, response);
    }
}
