package org.certis.studyplatform.project.application.mapper;

import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project Application DTO Mapper
 *
 * Clean Architecture Application Layer
 * VO → DTO 변환 담당 (FacadeService에서만 사용)
 * Presentation Mapper에서 Application Layer로 이동됨
 */
@Component
public class ProjectApplicationDtoMapper {

    /**
     * ProjectVo를 ProjectDetailResponseDto로 변환
     * 새로운 ProjectVo 구조에 맞춰 매핑
     */
    public ProjectDetailResponseDto toProjectDetailResponseDto(ProjectVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectDetailResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .content(vo.content())
                .description(vo.description())
                .category(vo.category())
                .subCategory(vo.subCategory())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .creatorName(vo.creatorName())
                .githubUrl(vo.githubUrl())
                .attachedFiles(Collections.emptyList()) // 초기값은 빈 리스트, Facade에서 추가됨
                .meetingSummaries(Collections.emptyList()) // 초기값은 빈 리스트, Facade에서 추가됨
                .maxParticipants(vo.maxParticipants())
                .currentParticipants(vo.currentParticipants())
                .build();
    }

    /**
     * ProjectSummaryVo를 ProjectSummaryResponseDto로 변환
     */
    public ProjectSummaryResponseDto toProjectSummaryResponseDto(ProjectSummaryVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .description(vo.description())
                .category(vo.category())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .projectCreatorName(vo.projectCreatorName())
                .projectCreatorRole(vo.projectCreatorRole())
                .isParticipantable(vo.isParticipantable())
                .githubUrl(vo.githubUrl())
                .externalUrl(vo.externalUrl())
                .build();
    }

    /**
     * ProjectAttachedVo를 ProjectAttachedResponseDto로 변환
     */
    public ProjectAttachedResponseDto toProjectAttachedResponseDto(ProjectAttachedVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectAttachedResponseDto.builder()
                .id(vo.id())
                .name(vo.name())
                .type(vo.type())
                .size(vo.size())
                .attachedUrl(vo.attachedUrl())
                .build();
    }

    /**
     * ProjectAttachedVo 리스트를 ProjectAttachedResponseDto 리스트로 변환
     */
    public List<ProjectAttachedResponseDto> toProjectAttachedResponseDtoList(List<ProjectAttachedVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toProjectAttachedResponseDto)
                .toList();
    }

    /**
     * ProjectMeetingSummaryWithLinksVo를 ProjectMeetingSummaryResponseDto로 변환
     */
    public ProjectMeetingSummaryResponseDto toProjectMeetingSummaryResponseDto(ProjectMeetingSummaryWithLinksVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .isEditable(vo.isEditable())
                .build();
    }

    /**
     * ProjectMeetingSummaryVo를 ProjectMeetingSummaryResponseDto로 변환
     */
    public ProjectMeetingSummaryResponseDto toProjectMeetingSummaryResponseDto(ProjectMeetingSummaryVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .isEditable(vo.isEditable())
                .build();
    }

    /**
     * ProjectMeetingSummaryVo 리스트를 ProjectMeetingSummaryResponseDto 리스트로 변환
     */
    public List<ProjectMeetingSummaryResponseDto> toProjectMeetingSummaryResponseDtoList(List<ProjectMeetingSummaryVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toProjectMeetingSummaryResponseDto)
                .toList();
    }

    /**
     * ProjectMeetingPageResultVo를 ProjectMeetingSummaryResponseDto 리스트로 변환
     * 페이지 결과에서 실제 콘텐츠만 추출하여 DTO 리스트로 변환
     */
    public List<ProjectMeetingSummaryResponseDto> toProjectMeetingSummaryResponseDtoList(ProjectMeetingPageResultVo pageResultVo) {
        if (pageResultVo == null || pageResultVo.meetings() == null || pageResultVo.meetings().getContent().isEmpty()) {
            return Collections.emptyList();
        }

        return pageResultVo.meetings().getContent().stream()
                .map(this::toProjectMeetingSummaryResponseDto)
                .toList();
    }

    /**
     * ProjectSummaryVo 리스트를 ProjectSummaryResponseDto 리스트로 변환
     */
    public List<ProjectSummaryResponseDto> toProjectSummaryResponseDtoList(List<ProjectSummaryVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toProjectSummaryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * ProjectSummaryVo Page를 ProjectSummaryResponseDto Page로 변환
     */
    public Page<ProjectSummaryResponseDto> toProjectSummaryResponseDtoPage(Page<ProjectSummaryVo> voPage) {
        if (voPage == null) {
            return Page.empty();
        }

        List<ProjectSummaryResponseDto> dtoList = toProjectSummaryResponseDtoList(voPage.getContent());
        return new PageImpl<>(dtoList, voPage.getPageable(), voPage.getTotalElements());
    }

    /**
     * ProjectParticipantCreatedVo → ProjectJoinResponseDto 변환
     */
    public ProjectJoinResponseDto toProjectJoinResponseDto(ProjectParticipantCreatedVo vo) {
        return ProjectJoinResponseDto.builder()
                .participantId(vo.id())
                .projectId(vo.projectId())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .build();
    }

    /**
     * ProjectParticipantStatusUpdatedVo → ProjectParticipantStatusUpdateResponseDto 변환
     */
    public ProjectParticipantStatusUpdateResponseDto toProjectParticipantStatusUpdateResponseDto(
            ProjectParticipantStatusUpdatedVo vo) {

        String message = switch (vo.currentStatus()) {
            case APPROVED -> "프로젝트 참가가 승인되었습니다.";
            case REJECTED -> "프로젝트 참가가 거절되었습니다.";
            case CANCELLED -> "프로젝트 참가 신청이 취소되었습니다.";
            default -> "프로젝트 참가 상태가 변경되었습니다.";
        };

        return ProjectParticipantStatusUpdateResponseDto.builder()
                .participantId(vo.id())
                .projectId(vo.projectId())
                .memberId(vo.memberId())
                .previousStatus(vo.previousStatus())
                .currentStatus(vo.currentStatus())
                .message(message)
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * ProjectParticipantSummaryVo → ProjectParticipantSummaryResponseDto 변환
     */
    public ProjectParticipantSummaryResponseDto toProjectParticipantSummaryResponseDto(
            ProjectParticipantSummaryVo vo) {
        return ProjectParticipantSummaryResponseDto.builder()
                .id(vo.id())
                .memberId(vo.memberId())
                .memberName(vo.memberName())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .build();
    }

    /**
     * ProjectParticipantVo → ProjectParticipantDetailResponseDto 변환
     */
    public ProjectParticipantDetailResponseDto toProjectParticipantDetailResponseDto(
            ProjectParticipantVo vo, String projectTitle) {
        return ProjectParticipantDetailResponseDto.builder()
                .id(vo.id())
                .projectId(vo.projectId())
                .projectTitle(projectTitle)
                .memberId(vo.memberId())
                .memberName(vo.memberName())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * ProjectParticipantSummaryVo List → ProjectParticipantSummaryResponseDto List 변환
     */
    public List<ProjectParticipantSummaryResponseDto> toProjectParticipantSummaryResponseDtoList(
            List<ProjectParticipantSummaryVo> voList) {
        return voList.stream()
                .map(this::toProjectParticipantSummaryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Page<ProjectParticipantSummaryVo> → Page<ProjectParticipantSummaryResponseDto> 변환
     */
    public Page<ProjectParticipantSummaryResponseDto> toProjectParticipantSummaryResponseDtoPage(
            Page<ProjectParticipantSummaryVo> voPage) {
        List<ProjectParticipantSummaryResponseDto> dtoList = toProjectParticipantSummaryResponseDtoList(
                voPage.getContent());
        return new PageImpl<>(dtoList, voPage.getPageable(), voPage.getTotalElements());
    }

    /**
     * 프로젝트 참가자 통계 생성
     */
    public ProjectParticipantStatsResponseDto toProjectParticipantStatsResponseDto(
            Long projectId, Long approvedCount, Long pendingCount, Integer maxParticipants) {
        boolean isFull = maxParticipants != null && approvedCount >= maxParticipants;

        return ProjectParticipantStatsResponseDto.builder()
                .projectId(projectId)
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .maxParticipants(maxParticipants)
                .isFull(isFull)
                .build();
    }
}