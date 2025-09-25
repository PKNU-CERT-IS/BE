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
import org.certis.studyplatform.project.application.query.ProjectParticipantQueryService;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDeleteRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingAllRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.shared.service.S3FileService;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;



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
    private final ProjectParticipantQueryService projectParticipantQueryService;
    private final S3FileService s3FileService;

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
            request.getParticipantNumber(),
            request.getLinks()
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
        
        // 프로젝트 참가자 ID 목록 조회 (APPROVED 상태만)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<ProjectParticipantSummaryVo> participants = projectParticipantQueryService.getParticipantsByProject(
                meetingVo.projectId(), ProjectParticipantStatus.APPROVED, pageable);
        List<Long> participantIds = participants.getContent().stream()
                .map(ProjectParticipantSummaryVo::memberId)
                .toList();
        
        // 작성자 이름 조회 (참가자 목록에서 찾기)
        String writerName = participants.getContent().stream()
                .filter(p -> p.memberId().equals(meetingVo.writerId()))
                .findFirst()
                .map(ProjectParticipantSummaryVo::memberName)
                .orElse("알 수 없음");
        
        // VO → DTO 변환
        ProjectMeetingDetailResponseDto responseDto = ProjectMeetingDetailResponseDto.builder()
                .id(meetingVo.id())
                .projectId(meetingVo.projectId())
                .title(meetingVo.title())
                .content(meetingVo.content())
                .participantNumber(meetingVo.participantNumber())
                .participantIds(participantIds)
                .writerId(meetingVo.writerId())
                .writerName(writerName)
                .createdAt(meetingVo.createdAt())
                .updatedAt(meetingVo.updatedAt())
                .isEditable(meetingVo.isEditable())
                .links(meetingVo.attachedLinks() == null ? java.util.Collections.emptyList() : meetingVo.attachedLinks().stream()
                        .map(linkVo -> {
                            try {
                                var info = s3FileService.getObjectInfo(linkVo.attachedUrl());
                                if (info != null) {
                                    return ProjectMeetingDetailResponseDto.Link.builder()
                                            .title(info.getName())
                                            .url(info.getUrl())
                                            .build();
                                }
                            } catch (Exception ignored) {}
                            return ProjectMeetingDetailResponseDto.Link.builder()
                                    .title(linkVo.name())
                                    .url(linkVo.attachedUrl())
                                    .build();
                        }).toList())
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
            request.getParticipantNumber(),
            request.getLinks()
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
                        .createdAt(vo.createdAt())
                        .isEditable(vo.isEditable())
                        .links(vo.hasLinks() ? createMockLinks(vo.safeLinkCount()) : Collections.emptyList())
                        .build());

        log.info("MeetingFacade: Found {} meetings for project - ID: {}", result.getTotalElements(), request.getProjectId());
        
        return result;
    }

    /**
     * 프로젝트 회의록 목록 조회 (List 반환)
     *
     * @param projectId 프로젝트 ID
     * @return 회의록 목록 (List)
     */
    public List<ProjectMeetingSummaryResponseDto> getProjectMeetings(Long projectId) {
        log.info("MeetingFacade: Getting meetings for project - ID: {}", projectId);

        // 전체 조회를 위한 Pageable 생성
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        GetAllProjectMeetingsQuery query = GetAllProjectMeetingsQuery.of(projectId, pageable);
        
        // Query Service 호출
        ProjectMeetingPageResultVo meetingVos = projectMeetingQueryService.getAllProjectMeetings(query);
        
        // VO → DTO 변환 (List로 변환)
        List<ProjectMeetingSummaryResponseDto> meetings = meetingVos.meetings().getContent().stream()
            .map(vo -> ProjectMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .createdAt(vo.createdAt())
                .isEditable(vo.isEditable())
                .links(vo.hasLinks() ? createMockLinks(vo.safeLinkCount()) : Collections.emptyList())
                .build())
            .toList();

        log.info("MeetingFacade: Found {} meetings for project - ID: {}", meetings.size(), projectId);
        return meetings;
    }

    /**
     * 테스트용 링크 목록 생성
     */
    private List<ProjectMeetingSummaryResponseDto.Link> createMockLinks(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        
        return IntStream.range(0, count)
                .mapToObj(i -> ProjectMeetingSummaryResponseDto.Link.builder()
                        .title("회의록 첨부 링크 " + (i + 1))
                        .url("https://example.com/meeting-notes-" + (i + 1) + ".pdf")
                        .build())
                .toList();
    }
} 