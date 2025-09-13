package org.certis.studyplatform.study.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.study.application.StudyParticipantFacadeService;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.presentation.dto.request.*;
import org.certis.studyplatform.study.presentation.dto.response.*;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Study Participant Controller
 *
 * 프로젝트 참가 관련 REST API 컨트롤러
 * Clean Architecture Presentation Layer
 */
@RestController
@RequestMapping("/api/v1/study/participant")
@RequiredArgsConstructor
@Slf4j
public class
StudyParticipantController {

    private final StudyParticipantFacadeService studyParticipantFacadeService;

    // ================================================================
    // STUDY JOIN OPERATIONS - 프로젝트 참가 관리
    // ================================================================

    /**
     * 프로젝트 참가 신청
     *
     * @param requestDto 참가 신청 요청 DTO
     * @return 참가 신청 결과
     */
    @PostMapping("/join/register")
    public ResponseEntity<GlobalResponseHandler<StudyJoinResponseDto>> registerJoinStudy(
            @Valid @RequestBody StudyJoinRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {
        log.info("Controller: Register study join request - studyId: {}", requestDto.getStudyId());

        StudyJoinResponseDto responseDto = studyParticipantFacadeService.registerJoinStudy(requestDto, currentUser.getId());

        log.info("Controller: Study join registered successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_JOIN_REGISTERED, responseDto);
    }

    /**
     * 프로젝트 참가 신청 취소
     *
     * @param requestDto 참가 신청 취소 요청 DTO
     * @return 취소 결과
     */
    @DeleteMapping("/join/cancel")
    public ResponseEntity<GlobalResponseHandler<Void>> cancelJoinStudy(
            @Valid @RequestBody StudyJoinCancelRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Cancel study join request - studyId: {}", requestDto.getStudyId());

        studyParticipantFacadeService.cancelJoinStudy(requestDto, currentUser.getId());

        log.info("Controller: Study join cancelled successfully - studyId: {}",
                requestDto.getStudyId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_JOIN_CANCELED);
    }

    /**
     * 프로젝트 참가 승인
     *
     * @param requestDto 참가 승인 요청 DTO
     * @return 승인 결과
     */
    @PostMapping("/join/approve")
    public ResponseEntity<GlobalResponseHandler<StudyParticipantStatusUpdateResponseDto>> approveJoinStudy(
            @Valid @RequestBody StudyJoinApproveRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Approve study join request - participantId: {}",
                requestDto.getParticipantId());

        StudyParticipantStatusUpdateResponseDto responseDto =
                studyParticipantFacadeService.approveJoinStudy(requestDto, currentUser.getId());

        log.info("Controller: Study join approved successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_JOIN_APPROVED, responseDto);
    }

    /**
     * 프로젝트 참가 거절
     *
     * @param requestDto 참가 거절 요청 DTO
     * @return 거절 결과
     */
    @PostMapping("/join/reject")
    public ResponseEntity<GlobalResponseHandler<StudyParticipantStatusUpdateResponseDto>> rejectJoinStudy(
            @Valid @RequestBody StudyJoinRejectRequestDto requestDto,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        log.info("Controller: Reject study join request - participantId: {}",
                requestDto.getParticipantId());

        StudyParticipantStatusUpdateResponseDto responseDto =
                studyParticipantFacadeService.rejectJoinStudy(requestDto, currentUser.getId());

        log.info("Controller: Study join rejected successfully - participantId: {}",
                responseDto.getParticipantId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_JOIN_REJECTED, responseDto);
    }

    // ================================================================
    // STUDY PRESENTATION QUERY OPERATIONS - 프로젝트 참가자 조회
    // ================================================================

    /**
     * 프로젝트별 참가자 목록 조회
     *
     * @param studyId 프로젝트 ID
     * @param status 참가자 상태 (선택적)
     * @param pageable 페이징 정보
     * @return 참가자 목록
     */
    @GetMapping("/{studyId}/participants")
    public ResponseEntity<GlobalResponseHandler<Page<StudyParticipantSummaryResponseDto>>> getStudyParticipants(
            @PathVariable Long studyId,
            @RequestParam(required = false) StudyParticipantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get study participants - studyId: {}, status: {}", studyId, status);

        Page<StudyParticipantSummaryResponseDto> participants =
                studyParticipantFacadeService.getStudyParticipants(studyId, status, pageable);

        log.info("Controller: Found {} study participants", participants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_SEARCH_SUCCESS, participants);
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     *
     * @param studyId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 모든 참가자 목록
     */
    @GetMapping("/{studyId}/participants/all")
    public ResponseEntity<GlobalResponseHandler<Page<StudyParticipantSummaryResponseDto>>> getAllStudyParticipants(
            @PathVariable Long studyId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get all study participants - studyId: {}", studyId);

        Page<StudyParticipantSummaryResponseDto> participants =
                studyParticipantFacadeService.getAllStudyParticipants(studyId, pageable);

        log.info("Controller: Found {} total study participants", participants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_SEARCH_SUCCESS, participants);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     *
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 참가 프로젝트 목록
     */
    @GetMapping("/members/{memberId}/participations")
    public ResponseEntity<GlobalResponseHandler<Page<StudyParticipantSummaryResponseDto>>> getMemberParticipations(
            @PathVariable Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get member participations - memberId: {}", memberId);

        Page<StudyParticipantSummaryResponseDto> participations =
                studyParticipantFacadeService.getMemberParticipations(memberId, pageable);

        log.info("Controller: Found {} member participations", participations.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_SEARCH_SUCCESS, participations);
    }


    // ================================================================
    // CONVENIENCE ENDPOINTS - 편의 기능
    // ================================================================

    /**
     * 프로젝트별 대기 중인 참가 신청 목록 조회 (프로젝트 생성자용)
     *
     * @param studyId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 대기 중인 참가 신청 목록
     */
    @GetMapping("/{studyId}/participants/pending")
    public ResponseEntity<GlobalResponseHandler<Page<StudyParticipantSummaryResponseDto>>> getPendingParticipants(
            @PathVariable Long studyId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get pending participants - studyId: {}", studyId);

        Page<StudyParticipantSummaryResponseDto> pendingParticipants =
                studyParticipantFacadeService.getStudyParticipants(studyId, StudyParticipantStatus.PENDING, pageable);

        log.info("Controller: Found {} pending participants", pendingParticipants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_SEARCH_SUCCESS, pendingParticipants);
    }

    /**
     * 프로젝트별 승인된 참가자 목록 조회
     *
     * @param studyId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 승인된 참가자 목록
     */
    @GetMapping("/{studyId}/participants/approved")
    public ResponseEntity<GlobalResponseHandler<Page<StudyParticipantSummaryResponseDto>>> getApprovedParticipants(
            @PathVariable Long studyId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Controller: Get approved participants - studyId: {}", studyId);

        Page<StudyParticipantSummaryResponseDto> approvedParticipants =
                studyParticipantFacadeService.getStudyParticipants(studyId, StudyParticipantStatus.APPROVED, pageable);

        log.info("Controller: Found {} approved participants", approvedParticipants.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_PARTICIPANT_SEARCH_SUCCESS, approvedParticipants);
    }
}