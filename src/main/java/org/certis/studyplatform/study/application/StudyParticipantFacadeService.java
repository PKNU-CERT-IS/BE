package org.certis.studyplatform.study.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.study.application.query.StudyQueryService;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinApproveRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinCancelRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRejectRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.AdminStudyParticipantApprovalRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyJoinResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyParticipantStatusUpdateResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.AdminStudyParticipantApprovalResponseDto;
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
    private final MemberQueryService memberQueryService;
    private final StudyQueryService studyQueryService;

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

        log.info("Facade: Study join registered successfully - studyId: {}", responseDto.getStudyId());
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
        log.info("Facade: Approving study join - studyId: {}, memberId: {}", requestDto.getStudyId(), requestDto.getMemberId());

        // 권한 검증: 스터디 생성자 또는 관리자(STAFF 이상)만 승인 가능
        StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(requestDto.getStudyId()));
        boolean isLeader = studyVo.creatorId().equals(currentUserId);
        MemberVo requesterMember = memberQueryService.getMemberById(new GetMemberByIdQuery(currentUserId));
        boolean isAdmin = requesterMember != null
                && requesterMember.role() != null
                && MemberRole.isStaffOrAbove(requesterMember.role());
        if (!(isLeader || isAdmin)) {
            throw new ApplicationException(
                    ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }

        // 참가 신청 resolve: (studyId, memberId) → participantId
        var participantVo = participantQueryService
                .getByStudyIdAndMemberId(requestDto.getStudyId(), requestDto.getMemberId())
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // Command 생성 및 호출
        UpdateStudyParticipantStatusCommand command = new UpdateStudyParticipantStatusCommand(
                participantVo.id(),
                StudyParticipantStatus.APPROVED,
                currentUserId
        );
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // Response 변환
        StudyParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toStudyParticipantStatusUpdateResponseDto(updatedVo);
        log.info("Facade: Study join approved successfully - participantId: {}", updatedVo.id());
        return responseDto;
    }

    /**
     * 프로젝트 참가 거절
     */
    public StudyParticipantStatusUpdateResponseDto rejectJoinStudy(StudyJoinRejectRequestDto requestDto, Long currentUserId) {
        log.info("Facade: Rejecting study join - studyId: {}, memberId: {}", requestDto.getStudyId(), requestDto.getMemberId());

        // 권한 검증: 스터디 생성자 또는 관리자(STAFF 이상)만 거절 가능
        StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(requestDto.getStudyId()));
        boolean isLeader = studyVo.creatorId().equals(currentUserId);
        MemberVo requesterMember = memberQueryService.getMemberById(new GetMemberByIdQuery(currentUserId));
        boolean isAdmin = requesterMember != null
                && requesterMember.role() != null
                && MemberRole.isStaffOrAbove(requesterMember.role());
        if (!(isLeader || isAdmin)) {
            throw new ApplicationException(
                    ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }

        // 참가 신청 resolve: (studyId, memberId) → participantId
        var participantVo = participantQueryService
                .getByStudyIdAndMemberId(requestDto.getStudyId(), requestDto.getMemberId())
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // Command 생성 및 호출
        UpdateStudyParticipantStatusCommand command = new UpdateStudyParticipantStatusCommand(
                participantVo.id(),
                StudyParticipantStatus.REJECTED,
                currentUserId
        );
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // Response 변환
        StudyParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toStudyParticipantStatusUpdateResponseDto(updatedVo);
        log.info("Facade: Study join rejected successfully - participantId: {}", updatedVo.id());
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

    // ================================================================
    // ADMIN STUDY PARTICIPANT OPERATIONS - 관리자 스터디 참가 관리
    // ================================================================

    /**
     * 관리자가 스터디 참가 신청을 승인
     */
    public AdminStudyParticipantApprovalResponseDto approveParticipantByAdmin(
            AdminStudyParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin approving study participant - adminId: {}",
                adminId);

        // DTO → Command Object 변환
        UpdateStudyParticipantStatusCommand command = commandMapper
                .toApproveStudyParticipantByAdminCommand(request, adminId);

        // Command Service 호출
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // 조회: 스터디 제목, 멤버 이름, 관리자 이름
        String studyTitle = null;
        String memberName = null;
        String adminName = null;

        if (updatedVo.studyId() != null) {
            StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(updatedVo.studyId()));
            studyTitle = studyVo != null ? studyVo.title() : null;
        }
        if (updatedVo.memberId() != null) {
            MemberVo memberVo = memberQueryService.getMemberById(new GetMemberByIdQuery(updatedVo.memberId()));
            memberName = memberVo != null ? memberVo.name() : null;
        }
        if (adminId != null) {
            MemberVo adminVo = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
            adminName = adminVo != null ? adminVo.name() : null;
        }

        // VO → Response DTO 변환 (실데이터 적용)
        AdminStudyParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminStudyParticipantApprovalResponseDto(
                        updatedVo,
                        studyTitle,
                        memberName,
                        StudyParticipantStatus.APPROVED,
                        adminId,
                        adminName
                );

        log.info("Facade: Admin study participant approved successfully - participantId: {}", 
                responseDto.getStudyId());
        return responseDto;
    }

    /**
     * 관리자가 스터디 참가 신청을 거절
     */
    public AdminStudyParticipantApprovalResponseDto rejectParticipantByAdmin(
            AdminStudyParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin rejecting study participant - adminId: {}", adminId);

        // DTO → Command Object 변환
        UpdateStudyParticipantStatusCommand command = commandMapper
                .toRejectStudyParticipantByAdminCommand(request, adminId);

        // Command Service 호출
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // 조회: 스터디 제목, 멤버 이름, 관리자 이름
        String studyTitle = null;
        String memberName = null;
        String adminName = null;

        if (updatedVo.studyId() != null) {
            StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(updatedVo.studyId()));
            studyTitle = studyVo != null ? studyVo.title() : null;
        }
        if (updatedVo.memberId() != null) {
            MemberVo memberVo = memberQueryService.getMemberById(new GetMemberByIdQuery(updatedVo.memberId()));
            memberName = memberVo != null ? memberVo.name() : null;
        }
        if (adminId != null) {
            MemberVo adminVo = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
            adminName = adminVo != null ? adminVo.name() : null;
        }

        // VO → Response DTO 변환 (실데이터 적용)
        AdminStudyParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminStudyParticipantApprovalResponseDto(
                        updatedVo,
                        studyTitle,
                        memberName,
                        StudyParticipantStatus.REJECTED,
                        adminId,
                        adminName
                );

        log.info("Facade: Admin study participant rejected successfully - participantId: {}", 
                responseDto.getStudyId());
        return responseDto;
    }

    /**
     * 관리자가 스터디 참가 신청을 승인 (studyId, memberId 사용)
     */
    public AdminStudyParticipantApprovalResponseDto approveParticipantByAdminWithStudyAndMember(
            AdminStudyParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin approving study participant with studyId and memberId - studyId: {}, memberId: {}, adminId: {}", 
                request.getStudyId(), request.getMemberId(), adminId);

        // 1. 참가자 정보 조회 (studyId, memberId로)
        StudyParticipantVo participantVo = participantQueryService
                .getByStudyIdAndMemberId(request.getStudyId(), request.getMemberId())
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다"));

        // 2. 스터디 정보 조회
        StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(participantVo.studyId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. Command 생성 및 호출
        UpdateStudyParticipantStatusCommand command = new UpdateStudyParticipantStatusCommand(
                participantVo.id(),
                StudyParticipantStatus.APPROVED,
                adminId
        );
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // 5. Response DTO 변환
        AdminStudyParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminStudyParticipantApprovalResponseDto(
                        updatedVo,
                        studyVo.title(),
                        participantVo.memberName(),
                        StudyParticipantStatus.APPROVED,
                        adminId,
                        adminName
                );

        log.info("Facade: Admin approved study participant successfully with studyId and memberId - studyId: {}, memberId: {}", 
                request.getStudyId(), request.getMemberId());
        return responseDto;
    }

    /**
     * 관리자가 스터디 참가 신청을 거절 (studyId, memberId 사용)
     */
    public AdminStudyParticipantApprovalResponseDto rejectParticipantByAdminWithStudyAndMember(
            AdminStudyParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin rejecting study participant with studyId and memberId - studyId: {}, memberId: {}, adminId: {}", 
                request.getStudyId(), request.getMemberId(), adminId);

        // 1. 참가자 정보 조회 (studyId, memberId로)
        StudyParticipantVo participantVo = participantQueryService
                .getByStudyIdAndMemberId(request.getStudyId(), request.getMemberId())
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다"));

        // 2. 스터디 정보 조회
        StudyVo studyVo = studyQueryService.getStudyById(GetStudyByIdQuery.of(participantVo.studyId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. Command 생성 및 호출
        UpdateStudyParticipantStatusCommand command = new UpdateStudyParticipantStatusCommand(
                participantVo.id(),
                StudyParticipantStatus.REJECTED,
                adminId
        );
        StudyParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // 5. Response DTO 변환
        AdminStudyParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminStudyParticipantApprovalResponseDto(
                        updatedVo,
                        studyVo.title(),
                        participantVo.memberName(),
                        StudyParticipantStatus.REJECTED,
                        adminId,
                        adminName
                );

        log.info("Facade: Admin rejected study participant successfully with studyId and memberId - studyId: {}, memberId: {}", 
                request.getStudyId(), request.getMemberId());
        return responseDto;
    }
}
