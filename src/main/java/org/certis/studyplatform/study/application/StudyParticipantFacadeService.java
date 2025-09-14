package org.certis.studyplatform.study.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinApproveRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinCancelRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRejectRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyJoinResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyParticipantStatusUpdateResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyParticipantSummaryResponseDto;
import org.certis.studyplatform.study.application.command.StudyParticipantCommandService;
import org.certis.studyplatform.study.application.mapper.StudyApplicationCommandMapper;
import org.certis.studyplatform.study.application.mapper.StudyApplicationDtoMapper;
import org.certis.studyplatform.study.application.object.command.CancelStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.CreateStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyParticipantStatusCommand;
import org.certis.studyplatform.study.application.query.StudyParticipantQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyParticipantFacadeService {
    // StudyFacadeService에 추가할 프로젝트 참가 관련 메소드들

    // Facade Service에 추가할 의존성들
    private final StudyParticipantCommandService participantCommandService;
    private final StudyParticipantQueryService participantQueryService;
    private final StudyApplicationCommandMapper commandMapper;
    private final StudyApplicationDtoMapper dtoMapper;

    // ================================================================
    // STUDY PARTICIPANT OPERATIONS - 프로젝트 참가 관리
    // ================================================================

    /**
     * 프로젝트 참가 신청
     */
    public StudyJoinResponseDto registerJoinStudy(StudyJoinRequestDto requestDto, Long currentUserId) {
        log.info("Facade: Registering study join - studyId: {}", requestDto.getStudyId());

        // DTO → Command Object 변환
        CreateStudyParticipantCommand command = commandMapper
                .toCreateStudyParticipantCommand(requestDto, currentUserId);

        // Command Service 호출
        StudyParticipantCreatedVo createdVo = participantCommandService.createParticipant(command);

        // VO → Response DTO 변환
        StudyJoinResponseDto responseDto = dtoMapper.toStudyJoinResponseDto(createdVo);

        log.info("Facade: Study join registered successfully - participantId: {}", responseDto.getParticipantId());
        return responseDto;
    }

    /**
     * 프로젝트 참가 신청 취소
     */
    public void cancelJoinStudy(StudyJoinCancelRequestDto requestDto, Long currentUserId) {
        log.info("Facade: Cancelling study join - studyId: {}", requestDto.getStudyId());

        // DTO → Command Object 변환
        CancelStudyParticipantCommand command = commandMapper
                .toCancelStudyParticipantCommand(requestDto, currentUserId);

        // Command Service 호출
        participantCommandService.cancelParticipant(command);

        log.info("Facade: Study join cancelled successfully - studyId: {}", requestDto.getStudyId());
    }

    /**
     * 프로젝트 참가 승인
     */
    public StudyParticipantStatusUpdateResponseDto approveJoinStudy(StudyJoinApproveRequestDto requestDto, Long currentUserId) {
        log.info("Facade: Approving study join - participantId: {}", requestDto.getParticipantId());

        // DTO → Command Object 변환
        UpdateStudyParticipantStatusCommand command = commandMapper
                .toApproveStudyParticipantCommand(requestDto, currentUserId);

        // Command Service 호출
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // VO → Response DTO 변환
        StudyParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toStudyParticipantStatusUpdateResponseDto(updatedVo);

        log.info("Facade: Study join approved successfully - participantId: {}", responseDto.getParticipantId());
        return responseDto;
    }

    /**
     * 프로젝트 참가 거절
     */
    public StudyParticipantStatusUpdateResponseDto rejectJoinStudy(StudyJoinRejectRequestDto requestDto, Long currentUserId) {
        log.info("Facade: Rejecting study join - participantId: {}", requestDto.getParticipantId());

        // DTO → Command Object 변환
        UpdateStudyParticipantStatusCommand command = commandMapper
                .toRejectStudyParticipantCommand(requestDto, currentUserId);

        // Command Service 호출
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // VO → Response DTO 변환
        StudyParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toStudyParticipantStatusUpdateResponseDto(updatedVo);

        log.info("Facade: Study join rejected successfully - participantId: {}", responseDto.getParticipantId());
        return responseDto;
    }

    // ================================================================
    // STUDY PARTICIPANT QUERY OPERATIONS - 프로젝트 참가자 조회
    // ================================================================

    /**
     * 프로젝트별 참가자 목록 조회 (상태별 필터링 가능)
     */
    public Page<StudyParticipantSummaryResponseDto> getStudyParticipants(
            Long studyId, StudyParticipantStatus status, Pageable pageable) {
        log.info("Facade: Getting study participants - studyId: {}, status: {}", studyId, status);

        // Query Service 호출
        Page<StudyParticipantSummaryVo> participantsVo = participantQueryService
                .getParticipantsByStudy(studyId, status, pageable);

        // VO → DTO 변환
        Page<StudyParticipantSummaryResponseDto> responseDto = dtoMapper
                .toStudyParticipantSummaryResponseDtoPage(participantsVo);

        log.info("Facade: Found {} study participants", responseDto.getTotalElements());
        return responseDto;
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     */
    public Page<StudyParticipantSummaryResponseDto> getAllStudyParticipants(Long studyId, Pageable pageable) {
        log.info("Facade: Getting all study participants - studyId: {}", studyId);

        return getStudyParticipants(studyId, null, pageable);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     */
    public Page<StudyParticipantSummaryResponseDto> getMemberParticipations(Long memberId, Pageable pageable) {
        log.info("Facade: Getting member participations - memberId: {}", memberId);

        // Query Service 호출
        Page<StudyParticipantSummaryVo> participationsVo = participantQueryService
                .getParticipantsByMember(memberId, pageable);

        // VO → DTO 변환
        Page<StudyParticipantSummaryResponseDto> responseDto = dtoMapper
                .toStudyParticipantSummaryResponseDtoPage(participationsVo);

        log.info("Facade: Found {} member participations", responseDto.getTotalElements());
        return responseDto;
    }
}
