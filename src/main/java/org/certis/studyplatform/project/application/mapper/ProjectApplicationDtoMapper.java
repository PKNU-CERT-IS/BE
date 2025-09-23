package org.certis.studyplatform.project.application.mapper;

import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                .creatorId(vo.creatorId())
                .projectCreatorName(vo.creatorName())
                .projectCreatorGrade(vo.creatorGrade())
                .semester(vo.semester())
                .status(vo.status() != null ? vo.status().toString() : null)
                .githubUrl(vo.githubUrl())
                .externalUrl(vo.externalUrl() != null ? 
                    ExternalUrlResponseDto.builder()
                        .title(vo.externalUrl().title())
                        .url(vo.externalUrl().url())
                        .build() : null)
                .demoUrl(vo.demoUrl())
                .thumbnailUrl(vo.thumbnailUrl())
                .attachments(vo.attached() != null ? toProjectAttachedResponseDtoList(vo.attached()) : Collections.emptyList()) // VO에서 첨부파일 정보 가져오기
                .meetingSummaries(Collections.emptyList()) // 초기값은 빈 리스트, Facade에서 추가됨
                .maxParticipantNumber(vo.maxParticipants())
                .currentParticipantNumber(vo.currentParticipants())
                .isParticipantable(vo.isParticipantable())
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
                .subcategory(vo.subcategory())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .projectCreatorName(vo.projectCreatorName())
                .projectCreatorGrade(vo.projectCreatorGrade())
                .semester(vo.semester())
                .status(vo.status())
                .isParticipantable(vo.isParticipantable())
                .githubUrl(vo.githubUrl())
                .externalUrl(vo.externalUrl() != null ? 
                    new ExternalUrlResponseDto(vo.externalUrl().title(), vo.externalUrl().url()) : null)
                .demoUrl(vo.demoUrl())
                .thumbnailUrl(vo.thumbnailUrl())
                .maxParticipantNumber(vo.maxParticipantNumber())
                .currentParticipantNumber(vo.currentParticipantNumber())
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
                .links(vo.hasLinks() ? createMockLinks(vo.safeLinkCount()) : Collections.emptyList())
                .build();
    }

    /**
     * ProjectMeetingSummaryVo를 ProjectMeetingSummaryResponseDto로 변환
     */
    public ProjectMeetingSummaryResponseDto toProjectMeetingSummaryResponseDto(ProjectMeetingSummaryVo vo) {
        if (vo == null) {
            return null;
        }

        // 기존 meetingAttachedUrl과 meetingAttachedTitle을 links로 변환
        List<ProjectMeetingSummaryResponseDto.Link> links = Collections.emptyList();
        if (vo.meetingAttachedUrl() != null && !vo.meetingAttachedUrl().isEmpty()) {
            links = List.of(ProjectMeetingSummaryResponseDto.Link.builder()
                    .title(vo.meetingAttachedTitle() != null ? vo.meetingAttachedTitle() : "회의록 첨부 링크")
                    .url(vo.meetingAttachedUrl())
                    .build());
        }

        return ProjectMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .isEditable(vo.isEditable())
                .links(links)
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
                .memberGrade(vo.memberGrade())
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

    /**
     * AdminProjectParticipantApprovalResponseDto 생성
     * 
     * @param participantVo 참가자 정보 VO
     * @param projectVo 프로젝트 정보 VO
     * @param adminId 관리자 ID
     * @param adminName 관리자 이름
     * @param reason 승인/거절 사유
     * @param processedAt 처리 시간
     * @return AdminProjectParticipantApprovalResponseDto
     */
    public AdminProjectParticipantApprovalResponseDto toAdminProjectParticipantApprovalResponseDto(
            ProjectParticipantVo participantVo,
            ProjectVo projectVo,
            Long adminId,
            String adminName,
            String reason,
            OffsetDateTime processedAt) {
        
        if (participantVo == null || projectVo == null) {
            return null;
        }

        return AdminProjectParticipantApprovalResponseDto.builder()
                .participantId(participantVo.id())
                .projectId(participantVo.projectId())
                .projectTitle(projectVo.title())
                .memberId(participantVo.memberId())
                .memberName(participantVo.memberName())
                .status(participantVo.status())
                .reason(reason)
                .adminId(adminId)
                .adminName(adminName)
                .processedAt(processedAt)
                .build();
    }

    /**
     * ProjectParticipantVo와 업데이트된 상태를 AdminProjectParticipantApprovalResponseDto로 변환
     * 
     * @param participantVo 참가자 정보 VO
     * @param projectVo 프로젝트 정보 VO
     * @param updatedStatus 업데이트된 상태
     * @param adminId 관리자 ID
     * @param adminName 관리자 이름
     * @param reason 승인/거절 사유
     * @param processedAt 처리 시간
     * @return AdminProjectParticipantApprovalResponseDto
     */
    public AdminProjectParticipantApprovalResponseDto toAdminProjectParticipantApprovalResponseDto(
            ProjectParticipantVo participantVo,
            ProjectVo projectVo,
            ProjectParticipantStatus updatedStatus,
            Long adminId,
            String adminName,
            String reason,
            OffsetDateTime processedAt) {
        
        if (participantVo == null || projectVo == null) {
            return null;
        }

        return AdminProjectParticipantApprovalResponseDto.builder()
                .participantId(participantVo.id())
                .projectId(participantVo.projectId())
                .projectTitle(projectVo.title())
                .memberId(participantVo.memberId())
                .memberName(participantVo.memberName())
                .status(updatedStatus)
                .reason(reason)
                .adminId(adminId)
                .adminName(adminName)
                .processedAt(processedAt)
                .build();
    }
}