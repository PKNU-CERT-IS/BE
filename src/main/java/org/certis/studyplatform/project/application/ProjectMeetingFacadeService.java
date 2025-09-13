package org.certis.studyplatform.project.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.command.ProjectMeetingCommandService;
import org.certis.studyplatform.project.application.object.command.CreateProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.query.GetAllProjectMeetingsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectMeetingByIdQuery;
import org.certis.studyplatform.project.application.query.ProjectMeetingQueryService;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDeleteRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingAllRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;



/**
 * Project Meeting Facade Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 관련 기능 전용 Facade Service
 * ProjectFacadeService에서 분리하여 독립적으로 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingFacadeService {

    private final ProjectMeetingCommandService projectMeetingCommandService;
    private final ProjectMeetingQueryService projectMeetingQueryService;

    // ================================================================
    // PROJECT MEETING OPERATIONS - 회의록 관리
    // ================================================================

    /**
     * 프로젝트 회의록 생성
     *
     * @param request 회의록 생성 요청 DTO
     */
    public void createProjectMeeting(ProjectMeetingCreateRequestDto request,Long writerId
    ) {
        log.info("MeetingFacade: Creating project meeting - projectId: {}, title: {}", 
                request.getProjectId(), request.getTitle());
        
        // DTO → Command 변환
        CreateProjectMeetingCommand command = CreateProjectMeetingCommand.of(
            request.getProjectId(),
            writerId,
            request.getTitle(),
            request.getContent(),
            request.getParticipantIds(),
            request.getAttachedUrl()
        );
        
        // Command Service 호출
        ProjectMeetingCreatedVo createdVo = projectMeetingCommandService.createProjectMeeting(command);
        
        log.info("MeetingFacade: Project meeting created successfully - ID: {}", createdVo.id());
    }

    /**
     * 프로젝트 회의록 상세 조회
     *
     * @param request 회의록 상세 조회 요청 DTO
     * @return 회의록 상세 정보
     */
    public ProjectMeetingDetailResponseDto getProjectMeetingDetail(ProjectMeetingDetailRequestDto request) {
        log.info("MeetingFacade: Getting project meeting detail - meetingId: {}", request.getMeetingId());
        
        // DTO → Query 변환
        GetProjectMeetingByIdQuery query = GetProjectMeetingByIdQuery.of(request.getMeetingId());
        
        // Query Service 호출
        ProjectMeetingDetailVo meetingVo = projectMeetingQueryService.getProjectMeetingById(query);
        
        // VO → DTO 변환
        ProjectMeetingDetailResponseDto responseDto = ProjectMeetingDetailResponseDto.builder()
                .id(meetingVo.id())
                .projectId(meetingVo.projectId())
                .title(meetingVo.title())
                .content(meetingVo.content())
                .participantIds(meetingVo.participantIds())
                .writerId(meetingVo.writerId())
                .createdAt(meetingVo.createdAt())
                .updatedAt(meetingVo.updatedAt())
                .isEditable(meetingVo.isEditable())
                .build();
        
        log.info("MeetingFacade: Project meeting detail retrieved successfully - ID: {}", responseDto.getId());
        return responseDto;
    }

    /**
     * 프로젝트 회의록 수정
     *
     * @param request 회의록 수정 요청 DTO
     */
    public void updateProjectMeeting(ProjectMeetingUpdateRequestDto request, Long requesterId) {
        log.info("MeetingFacade: Updating project meeting - meetingId: {}, requesterId: {}", 
                request.getMeetingId(), requesterId);
        
        // DTO → Command 변환
        UpdateProjectMeetingCommand command = UpdateProjectMeetingCommand.of(
            request.getMeetingId(),
                requesterId,
            request.getTitle(),
            request.getContent(),
            request.getParticipants(),
            request.getAttachedUrl()
        );
        
        // Command Service 호출
        ProjectMeetingUpdatedVo updatedVo = projectMeetingCommandService.updateProjectMeeting(command);
        
        log.info("MeetingFacade: Project meeting updated successfully - ID: {}", updatedVo.id());
    }

    /**
     * 프로젝트 회의록 삭제
     *
     * @param request 회의록 삭제 요청 DTO
     */
    public void deleteProjectMeeting(ProjectMeetingDeleteRequestDto request,Long requesterId) {
        log.info("MeetingFacade: Deleting project meeting - meetingId: {}, requesterId: {}", 
                request.getMeetingId(),requesterId);
        
        // DTO → Command 변환
        DeleteProjectMeetingCommand command = DeleteProjectMeetingCommand.of(
            request.getMeetingId(),
                requesterId
        );
        
        // Command Service 호출
        projectMeetingCommandService.deleteProjectMeeting(command);
        
        log.info("MeetingFacade: Project meeting deleted successfully");
    }

    /**
     * 프로젝트 회의록 전체 목록 조회 (페이징 지원)
     *
     * @param request 회의록 전체 목록 조회 요청 DTO
     * @param pageable 페이징 정보
     * @return 회의록 목록 (페이징)
     */
    public Page<ProjectMeetingSummaryResponseDto> getAllProjectMeetings(ProjectMeetingAllRequestDto request, Pageable pageable) {
        log.info("MeetingFacade: Getting all project meetings - projectId: {}, page: {}, size: {}", 
                request.getProjectId(), pageable.getPageNumber(), pageable.getPageSize());
        
        // DTO → Query 변환
        GetAllProjectMeetingsQuery query = GetAllProjectMeetingsQuery.of(request.getProjectId(), pageable);
        
        // Query Service 호출
        ProjectMeetingPageResultVo meetingVos = projectMeetingQueryService.getAllProjectMeetings(query);
        
        // VO → DTO 변환 (Page.map 사용으로 직접 변환)
        Page<ProjectMeetingSummaryResponseDto> result = meetingVos.meetings().map(vo ->
                ProjectMeetingSummaryResponseDto.builder()
                        .id(vo.id())
                        .title(vo.title())
                        .participantNumber(vo.participantNumber())
                        .creatorName(vo.creatorName())
                        .isEditable(vo.isEditable())
                        .build());

        log.info("MeetingFacade: Found {} meetings for project - ID: {}", result.getTotalElements(), request.getProjectId());
        
        return result;
    }
} 