package org.certis.studyplatform.project.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.command.ProjectParticipantCommandService;
import org.certis.studyplatform.project.application.mapper.ProjectApplicationCommandMapper;
import org.certis.studyplatform.project.application.mapper.ProjectApplicationDtoMapper;
import org.certis.studyplatform.project.application.object.command.CancelProjectParticipantCommand;
import org.certis.studyplatform.project.application.object.command.CreateProjectParticipantCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectParticipantStatusCommand;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.application.query.ProjectParticipantQueryService;
import org.certis.studyplatform.project.application.query.ProjectQueryService;
import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantStatusUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinApproveRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinCancelRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinRejectRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.AdminProjectParticipantApprovalRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectJoinResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectParticipantStatusUpdateResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.AdminProjectParticipantApprovalResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectParticipantSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectParticipantFacadeService {
    // ProjectFacadeService에 추가할 프로젝트 참가 관련 메소드들

    // Facade Service에 추가할 의존성들
    private final ProjectParticipantCommandService participantCommandService;
    private final ProjectParticipantQueryService participantQueryService;
    private final ProjectQueryService projectQueryService;
    private final MemberQueryService memberQueryService;
    private final ProjectApplicationCommandMapper commandMapper;
    private final ProjectApplicationDtoMapper dtoMapper;

    // ================================================================
    // PROJECT PARTICIPANT OPERATIONS - 프로젝트 참가 관리
    // ================================================================

    /**
     * 프로젝트 참가 신청
     */
    public ProjectJoinResponseDto registerJoinProject(ProjectJoinRequestDto requestDto, Long requesterId) {
        log.info("Facade: Registering project join - projectId: {}", requestDto.getProjectId());

        // 재신청 복원 시나리오: 소프트 삭제(REJECTED)된 최근 신청 복원 시 OK
        boolean restored = participantCommandService.restoreLatestSoftDeleted(requestDto.getProjectId(), requesterId);

        ProjectParticipantCreatedVo createdVo;
        if (!restored) {
            // DTO → Command Object 변환 후 신규 생성
            CreateProjectParticipantCommand command = commandMapper
                    .toCreateProjectParticipantCommand(requestDto, requesterId);
            // Command Service 호출
            createdVo = participantCommandService.createParticipant(command);
        } else {
            // 복원된 경우: 응답은 동일 구조이나 HTTP 200을 위해 createdAt을 null 로 설정
            createdVo = ProjectParticipantCreatedVo.of(
                    null,
                    requestDto.getProjectId(),
                    requesterId,
                    ProjectParticipantStatus.PENDING,
                    null
            );
        }

        // VO → Response DTO 변환
        ProjectJoinResponseDto responseDto = dtoMapper.toProjectJoinResponseDto(createdVo);

        log.info("Facade: Project join registered successfully - projectId: {}", responseDto.getProjectId());
        return responseDto;
    }

    /**
     * 프로젝트 참가 신청 취소
     */
    public void cancelJoinProject(ProjectJoinCancelRequestDto requestDto, Long requesterId) {
        log.info("Facade: Cancelling project join - projectId: {}", requestDto.getProjectId());

        // DTO → Command Object 변환
        CancelProjectParticipantCommand command = commandMapper
                .toCancelProjectParticipantCommand(requestDto, requesterId);

        // Command Service 호출
        participantCommandService.cancelParticipant(command);

        log.info("Facade: Project join cancelled successfully - projectId: {}", requestDto.getProjectId());
    }

    /**
     * 프로젝트 참가 승인
     */
    public ProjectParticipantStatusUpdateResponseDto approveJoinProject(ProjectJoinApproveRequestDto requestDto,  Long requesterId) {
        log.info("Facade: Approving project join - projectId: {}, memberId: {}", requestDto.getProjectId(), requestDto.getMemberId());

        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(
                requestDto.getProjectId(),
                requestDto.getMemberId(),
                requesterId
        );

        // Response 변환
        ProjectParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toProjectParticipantStatusUpdateResponseDto(updatedVo);
        log.info("Facade: Project join approved successfully - projectId: {}", responseDto.getProjectId());
        return responseDto;
    }

    /**
     * 프로젝트 참가 거절
     */
    public ProjectParticipantStatusUpdateResponseDto rejectJoinProject(ProjectJoinRejectRequestDto requestDto, Long requesterId) {
        log.info("Facade: Rejecting project join - projectId: {}, memberId: {}", requestDto.getProjectId(), requestDto.getMemberId());

        // 권한 검증: 프로젝트 생성자 또는 관리자(STAFF 이상)만 거절 가능
        ProjectVo projectVo = projectQueryService.getProjectById(GetProjectByIdQuery.of(requestDto.getProjectId()));
        boolean isLeader = projectVo.creatorId().equals(requesterId);
        var requesterMember = memberQueryService.getMemberById(new GetMemberByIdQuery(requesterId));
        boolean isAdmin = requesterMember != null
                && requesterMember.role() != null
                && MemberRole.isStaffOrAbove(requesterMember.role());
        if (!(isLeader || isAdmin)) {
            throw new ApplicationException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED,
                    "프로젝트 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }

        // 참가 신청 resolve: (projectId, memberId) → participantId
        var participantVo = participantQueryService
                .getByProjectIdAndMemberId(requestDto.getProjectId(), requestDto.getMemberId())
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // Command 생성 및 호출
        UpdateProjectParticipantStatusCommand command = new UpdateProjectParticipantStatusCommand(
                participantVo.id(),
                ProjectParticipantStatus.REJECTED,
                requesterId
        );
        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // Response 변환
        ProjectParticipantStatusUpdateResponseDto responseDto = dtoMapper
                .toProjectParticipantStatusUpdateResponseDto(updatedVo);
        log.info("Facade: Project join rejected successfully - projectId: {}", responseDto.getProjectId());
        return responseDto;
    }

    // ================================================================
    // PROJECT PARTICIPANT QUERY OPERATIONS - 프로젝트 참가자 조회
    // ================================================================

    /**
     * 프로젝트별 참가자 목록 조회 (상태별 필터링 가능)
     */
    public Page<ProjectParticipantSummaryResponseDto> getProjectParticipants(
            Long projectId, ProjectParticipantStatus status, Pageable pageable) {
        log.info("Facade: Getting project participants - projectId: {}, status: {}", projectId, status);

        // Query Service 호출
        Page<ProjectParticipantSummaryVo> participantsVo = participantQueryService
                .getParticipantsByProject(projectId, status, pageable);

        // VO → DTO 변환
        Page<ProjectParticipantSummaryResponseDto> responseDto = dtoMapper
                .toProjectParticipantSummaryResponseDtoPage(participantsVo);

        log.info("Facade: Found {} project participants", responseDto.getTotalElements());
        return responseDto;
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     */
    public Page<ProjectParticipantSummaryResponseDto> getAllProjectParticipants(Long projectId, Pageable pageable) {
        log.info("Facade: Getting all project participants - projectId: {}", projectId);

        return getProjectParticipants(projectId, null, pageable);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     */
    public Page<ProjectParticipantSummaryResponseDto> getMemberParticipations(Long memberId, Pageable pageable) {
        log.info("Facade: Getting member participations - memberId: {}", memberId);

        // Query Service 호출
        Page<ProjectParticipantSummaryVo> participationsVo = participantQueryService
                .getParticipantsByMember(memberId, pageable);

        // VO → DTO 변환
        Page<ProjectParticipantSummaryResponseDto> responseDto = dtoMapper
                .toProjectParticipantSummaryResponseDtoPage(participationsVo);

        log.info("Facade: Found {} member participations", responseDto.getTotalElements());
        return responseDto;
    }

    // ================================================================
    // ADMIN PROJECT PARTICIPANT OPERATIONS - 관리자 프로젝트 참가 관리
    // ================================================================

    /**
     * 관리자가 프로젝트 참가 신청을 승인
     */
    public AdminProjectParticipantApprovalResponseDto approveParticipantByAdmin(
            AdminProjectParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin approving project participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), adminId);

        // 1. 참가자 정보 조회
        ProjectParticipantVo participantVo = participantQueryService.getParticipantById(request.getParticipantId())
                .orElseThrow(() -> new IllegalArgumentException("참가 신청을 찾을 수 없습니다: " + request.getParticipantId()));

        // 2. 프로젝트 정보 조회
        ProjectVo projectVo = projectQueryService.getProjectById(GetProjectByIdQuery.of(participantVo.projectId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. DTO → Command Object 변환
        UpdateProjectParticipantStatusCommand command = commandMapper
                .toApproveProjectParticipantByAdminCommand(request, adminId);

        // 5. Command Service 호출
        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // 6. VO → Response DTO 변환 (업데이트된 상태 사용)
        AdminProjectParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminProjectParticipantApprovalResponseDto(
                        participantVo, projectVo, updatedVo.currentStatus(), adminId, adminName, 
                        request.getReason(), updatedVo.updatedAt());

        log.info("Facade: Admin approved project participant successfully - memberId: {}",
                responseDto.getMemberId());
        return responseDto;
    }

    /**
     * 관리자가 프로젝트 참가 신청을 거절
     */
    public AdminProjectParticipantApprovalResponseDto rejectParticipantByAdmin(
            AdminProjectParticipantApprovalRequestDto request, Long adminId) {
        log.info("Facade: Admin rejecting project participant - participantId: {}, adminId: {}", 
                request.getParticipantId(), adminId);

        // 1. 참가자 정보 조회
        ProjectParticipantVo participantVo = participantQueryService.getParticipantById(request.getParticipantId())
                .orElseThrow(() -> new IllegalArgumentException("참가 신청을 찾을 수 없습니다: " + request.getParticipantId()));

        // 2. 프로젝트 정보 조회
        ProjectVo projectVo = projectQueryService.getProjectById(GetProjectByIdQuery.of(participantVo.projectId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. DTO → Command Object 변환
        UpdateProjectParticipantStatusCommand command = commandMapper
                .toRejectProjectParticipantByAdminCommand(request, adminId);

        // 5. Command Service 호출
        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // 6. VO → Response DTO 변환 (업데이트된 상태 사용)
        AdminProjectParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminProjectParticipantApprovalResponseDto(
                        participantVo, projectVo, updatedVo.currentStatus(), adminId, adminName,
                        request.getReason(), updatedVo.updatedAt());

        log.info("Facade: Admin rejected project participant successfully - memberId: {}",
                responseDto.getMemberId());
        return responseDto;
    }

    /**
     * 관리자가 프로젝트 참가 신청을 승인 (projectId, memberId 사용)
     */
    public AdminProjectParticipantApprovalResponseDto approveParticipantByAdminWithProjectAndMember(
            ProjectJoinApproveRequestDto request, Long adminId) {
        log.info("Facade: Admin approving project participant with projectId and memberId - projectId: {}, memberId: {}, adminId: {}", 
                request.getProjectId(), request.getMemberId(), adminId);

        // 1. 참가자 정보 조회 (projectId, memberId로)
        ProjectParticipantVo participantVo = participantQueryService
                .getByProjectIdAndMemberId(request.getProjectId(), request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("참가 신청을 찾을 수 없습니다: projectId=" + request.getProjectId() + ", memberId=" + request.getMemberId()));

        // 2. 프로젝트 정보 조회
        ProjectVo projectVo = projectQueryService.getProjectById(GetProjectByIdQuery.of(participantVo.projectId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. Command 생성 및 호출
        UpdateProjectParticipantStatusCommand command = new UpdateProjectParticipantStatusCommand(
                participantVo.id(),
                ProjectParticipantStatus.APPROVED,
                adminId
        );
        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.approveParticipant(command);

        // 5. Response DTO 변환
        AdminProjectParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminProjectParticipantApprovalResponseDto(
                        participantVo, projectVo, updatedVo.currentStatus(), adminId, adminName,
                        "자격 요건 충족", updatedVo.updatedAt());

        log.info("Facade: Admin approved project participant successfully with projectId and memberId - projectId: {}, memberId: {}", 
                request.getProjectId(), request.getMemberId());
        return responseDto;
    }

    /**
     * 관리자가 프로젝트 참가 신청을 거절 (projectId, memberId 사용)
     */
    public AdminProjectParticipantApprovalResponseDto rejectParticipantByAdminWithProjectAndMember(
            ProjectJoinRejectRequestDto request, Long adminId) {
        log.info("Facade: Admin rejecting project participant with projectId and memberId - projectId: {}, memberId: {}, adminId: {}", 
                request.getProjectId(), request.getMemberId(), adminId);

        // 1. 참가자 정보 조회 (projectId, memberId로)
        ProjectParticipantVo participantVo = participantQueryService
                .getByProjectIdAndMemberId(request.getProjectId(), request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("참가 신청을 찾을 수 없습니다: projectId=" + request.getProjectId() + ", memberId=" + request.getMemberId()));

        // 2. 프로젝트 정보 조회
        ProjectVo projectVo = projectQueryService.getProjectById(GetProjectByIdQuery.of(participantVo.projectId()));

        // 3. 관리자 정보 조회
        MemberVo adminMember = memberQueryService.getMemberById(new GetMemberByIdQuery(adminId));
        String adminName = adminMember.name();

        // 4. Command 생성 및 호출
        UpdateProjectParticipantStatusCommand command = new UpdateProjectParticipantStatusCommand(
                participantVo.id(),
                ProjectParticipantStatus.REJECTED,
                adminId
        );
        ProjectParticipantStatusUpdatedVo updatedVo = participantCommandService.rejectParticipant(command);

        // 5. Response DTO 변환
        AdminProjectParticipantApprovalResponseDto responseDto = dtoMapper
                .toAdminProjectParticipantApprovalResponseDto(
                        participantVo, projectVo, updatedVo.currentStatus(), adminId, adminName,
                        "자격 요건 미충족", updatedVo.updatedAt());

        log.info("Facade: Admin rejected project participant successfully with projectId and memberId - projectId: {}, memberId: {}", 
                request.getProjectId(), request.getMemberId());
        return responseDto;
    }
}
