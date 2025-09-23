package org.certis.studyplatform.project.application.mapper;

import org.certis.studyplatform.project.application.object.command.*;
import org.certis.studyplatform.project.domain.vo.ExternalUrlVo;
import org.certis.studyplatform.project.presentation.dto.request.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Project Application Command Mapper
 *
 * Presentation Layer DTO를 Application Layer Command 객체로 변환
 */
@Component
public class ProjectApplicationCommandMapper {

    /**
     * ProjectCreateRequestDto를 CreateProjectCommand로 변환
     */
    public CreateProjectCommand toCreateProjectCommand(ProjectCreateRequestDto dto, Long creatorId) {
        // attachments 리스트를 변환합니다. (null-safe 처리 포함)
        List<CreateProjectAttachedCommand> attachedCommands =
                (dto.getAttachments() == null) ? null :
                        dto.getAttachments().stream()
                                .map(this::toCreateProjectAttachedCommand)
                                .collect(Collectors.toList());

        return CreateProjectCommand.of(
                dto.getTitle(),
                dto.getDescription(),
                dto.getContent(),
                dto.getCategory(),
                dto.getSubCategory(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getGithubUrl(),
                dto.getExternalUrl() != null ? 
                    new ExternalUrlVo(dto.getExternalUrl().getTitle(), dto.getExternalUrl().getUrl()) : null,
                dto.getDemoUrl(),
                dto.getThumbnailUrl(),
                attachedCommands,
                dto.getMaxParticipants(),
                creatorId
        );
    }

    /**
     * ProjectAttachedCreateRequestDto를 CreateProjectAttachedCommand로 변환 (이 메서드는 변경 없음)
     */
    public CreateProjectAttachedCommand toCreateProjectAttachedCommand(ProjectAttachedCreateRequestDto dto) {
        return CreateProjectAttachedCommand.of(
                dto.getName(),
                dto.getType(),
                dto.getSize(),
                dto.getAttachedUrl()
        );
    }

    /**
     * ProjectUpdateRequestDto를 UpdateProjectCommand로 변환
     */
    public UpdateProjectCommand toUpdateProjectCommand(ProjectUpdateRequestDto dto, Long requesterId) {
        // attachments 리스트를 변환합니다. (null-safe 처리 포함)
        List<CreateProjectAttachedCommand> attachedCommands =
                (dto.getAttachments() == null) ? null :
                        dto.getAttachments().stream()
                                .map(this::toCreateProjectAttachedCommand) // 람다식(메서드 참조)을 사용한 변환
                                .collect(Collectors.toList());

        return UpdateProjectCommand.of(
            dto.getProjectId(),
            dto.getTitle(),
            dto.getDescription(),
            dto.getContent(),
            dto.getCategory(),
            dto.getSubCategory(),
            dto.getStartDate(),
            dto.getEndDate(),
            dto.getGithubUrl(),
            dto.getExternalUrl() != null ? 
                new ExternalUrlVo(dto.getExternalUrl().getTitle(), dto.getExternalUrl().getUrl()) : null,
            dto.getDemoUrl(),
            dto.getThumbnailUrl(),
            attachedCommands,
            dto.getMaxParticipants(),
            requesterId
        );
    }

    /**
     * DeleteProjectCommand 생성
     */
    public DeleteProjectCommand toDeleteProjectCommand(Long projectId, Long requesterId) {
        return DeleteProjectCommand.of(projectId, requesterId);
    }

    /**
     * ProjectJoinRequestDto → CreateProjectParticipantCommand 변환
     */
    public CreateProjectParticipantCommand toCreateProjectParticipantCommand(
            ProjectJoinRequestDto requestDto, Long memberId) {
        return new CreateProjectParticipantCommand(
                requestDto.getProjectId(),
                memberId
        );
    }

    /**
     * ProjectJoinCancelRequestDto → CancelProjectParticipantCommand 변환
     */
    public CancelProjectParticipantCommand toCancelProjectParticipantCommand(
            ProjectJoinCancelRequestDto requestDto, Long memberId) {
        return new CancelProjectParticipantCommand(
                requestDto.getProjectId(),
                memberId
        );
    }

    /**
     * ProjectJoinApproveRequestDto → UpdateProjectParticipantStatusCommand 변환 (승인용)
     */
    public UpdateProjectParticipantStatusCommand toApproveProjectParticipantCommand(
            ProjectJoinApproveRequestDto requestDto, Long requesterId) {
        return new UpdateProjectParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED,
                requesterId
        );
    }

    /**
     * ProjectJoinRejectRequestDto → UpdateProjectParticipantStatusCommand 변환 (거절용)
     */
    public UpdateProjectParticipantStatusCommand toRejectProjectParticipantCommand(
            ProjectJoinRejectRequestDto requestDto, Long requesterId) {
        return new UpdateProjectParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.project.domain.ProjectParticipantStatus.REJECTED,
                requesterId
        );
    }

    /**
     * AdminProjectParticipantApprovalRequestDto → UpdateProjectParticipantStatusCommand 변환 (관리자 승인용)
     */
    public UpdateProjectParticipantStatusCommand toApproveProjectParticipantByAdminCommand(
            AdminProjectParticipantApprovalRequestDto requestDto, Long adminId) {
        return new UpdateProjectParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED,
                adminId
        );
    }

    /**
     * AdminProjectParticipantApprovalRequestDto → UpdateProjectParticipantStatusCommand 변환 (관리자 거절용)
     */
    public UpdateProjectParticipantStatusCommand toRejectProjectParticipantByAdminCommand(
            AdminProjectParticipantApprovalRequestDto requestDto, Long adminId) {
        return new UpdateProjectParticipantStatusCommand(
                requestDto.getParticipantId(),
                org.certis.studyplatform.project.domain.ProjectParticipantStatus.REJECTED,
                adminId
        );
    }

    /**
     * ProjectMeetingCreateRequestDto → CreateProjectMeetingCommand 변환
     */
    public CreateProjectMeetingCommand toCreateProjectMeetingCommand(
            ProjectMeetingCreateRequestDto requestDto, Long writerId) {
        return CreateProjectMeetingCommand.of(
                requestDto.getProjectId(),
                writerId,
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getParticipantNumber(),
                requestDto.getLinks()
        );
    }

    /**
     * ProjectMeetingUpdateRequestDto → UpdateProjectMeetingCommand 변환
     */
    public UpdateProjectMeetingCommand toUpdateProjectMeetingCommand(
            ProjectMeetingUpdateRequestDto requestDto, Long requesterId) {
        return UpdateProjectMeetingCommand.of(
                requestDto.getMeetingId(),
                requesterId,
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getParticipantNumber(),
                requestDto.getLinks()
        );
    }
}