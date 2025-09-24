package org.certis.studyplatform.study.application.mapper;

import org.certis.studyplatform.study.domain.vo.*;
import org.certis.studyplatform.study.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Study Application DTO Mapper
 *
 * Clean Architecture Application Layer
 * VO → DTO 변환 담당 (FacadeService에서만 사용)
 * Presentation Mapper에서 Application Layer로 이동됨
 */
@Component
public class StudyApplicationDtoMapper {

    /**
     * StudyVo를 StudyDetailResponseDto로 변환
     * 새로운 StudyVo 구조에 맞춰 매핑
     */
    public StudyDetailResponseDto toStudyDetailResponseDto(StudyVo vo) {
        if (vo == null) {
            return null;
        }

        String thumbnailUrl = null;
        if (vo.attached() != null) {
            thumbnailUrl = vo.attached().stream()
                    .filter(a -> a.type() != null && (
                            a.type().toLowerCase().startsWith("image/") ||
                            a.type().equalsIgnoreCase("png") ||
                            a.type().equalsIgnoreCase("jpg") ||
                            a.type().equalsIgnoreCase("jpeg")
                    ))
                    .map(StudyAttachedVo::attachedUrl)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        return StudyDetailResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .content(vo.content())
                .description(vo.description())
                .category(vo.category())
                .subCategory(vo.subCategory())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .creatorId(vo.creatorId())
                .studyCreatorName(vo.creatorName())
                .studyCreatorGrade(vo.creatorGrade() != null ? vo.creatorGrade().toString() : null)
                .semester(vo.semester())
                .status(vo.status())
                .resultSubmitStatus(vo.resultSubmitStatus())
                .thumbnailUrl(thumbnailUrl)
                .attachments(toStudyAttachedResponseDtoList(vo.attached()))
                .meetingSummaries(toStudyMeetingSummaryResponseDtoList(vo.summaryVoList()))
                .participantSummaries(toStudyParticipantSummaryResponseDtoListFromVo(vo.participantVoList()))
                .maxParticipantNumber(vo.maxParticipants())
                .currentParticipantNumber(vo.currentParticipants())
                .isParticipantable(vo.isParticipantable())
                .build();
    }

    /**
     * StudySummaryVo를 StudySummaryResponseDto로 변환
     */
    public StudySummaryResponseDto toStudySummaryResponseDto(StudySummaryVo vo) {
        if (vo == null) {
            return null;
        }

        String thumbnailUrl = null;
        if (vo.attachedVo() != null) {
            thumbnailUrl = vo.attachedVo().stream()
                    .filter(a -> a.type() != null && (
                            a.type().toLowerCase().startsWith("image/") ||
                            a.type().equalsIgnoreCase("png") ||
                            a.type().equalsIgnoreCase("jpg") ||
                            a.type().equalsIgnoreCase("jpeg")
                    ))
                    .map(StudyAttachedVo::attachedUrl)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        return StudySummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .description(vo.description())
                .category(vo.category())
                .subcategory(vo.subcategory())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .studyCreatorName(vo.studyCreatorName())
                .studyCreatorGrade(vo.studyCreatorGrade())
                .semester(vo.semester())
                .status(vo.status())
                .resultSubmitStatus(null)
                .isParticipantable(vo.isParticipantable())
                .currentParticipantNumber(vo.currentParticipants())
                .maxParticipantNumber(vo.maxParticipants())
                .thumbnailUrl(thumbnailUrl)
                .attachments(toStudyAttachedResponseDtoList(vo.attachedVo()))
                .build();
    }

    /**
     * StudyAttachedVo를 StudyAttachedResponseDto로 변환
     */
    public StudyAttachedResponseDto toStudyAttachedResponseDto(StudyAttachedVo vo) {
        if (vo == null) {
            return null;
        }

        return StudyAttachedResponseDto.builder()
                .id(vo.id())
                .name(vo.name())
                .type(vo.type())
                .size(vo.size())
                .attachedUrl(vo.attachedUrl())
                .build();
    }

    /**
     * StudyAttachedVo 리스트를 StudyAttachedResponseDto 리스트로 변환
     */
    public List<StudyAttachedResponseDto> toStudyAttachedResponseDtoList(List<StudyAttachedVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toStudyAttachedResponseDto)
                .toList();
    }

    /**
     * StudyMeetingSummaryWithLinksVo를 StudyMeetingSummaryResponseDto로 변환
     */
    public StudyMeetingSummaryResponseDto toStudyMeetingSummaryResponseDto(StudyMeetingSummaryWithLinksVo vo) {
        if (vo == null) {
            return null;
        }

        return StudyMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .isEditable(vo.isEditable())
                .links(vo.hasLinks() ? createMockLinks(vo.safeLinkCount()) : Collections.emptyList())
                .build();
    }

    /**
     * StudyMeetingSummaryVo를 StudyMeetingSummaryResponseDto로 변환
     */
    public StudyMeetingSummaryResponseDto toStudyMeetingSummaryResponseDto(StudyMeetingSummaryVo vo) {
        if (vo == null) {
            return null;
        }

        // 기존 meetingAttachedUrl과 meetingAttachedTitle을 links로 변환
        List<StudyMeetingSummaryResponseDto.Link> links = Collections.emptyList();
        if (vo.meetingAttachedUrl() != null && !vo.meetingAttachedUrl().isEmpty()) {
            links = List.of(StudyMeetingSummaryResponseDto.Link.builder()
                    .title(vo.meetingAttachedTitle() != null ? vo.meetingAttachedTitle() : "회의록 첨부 링크")
                    .url(vo.meetingAttachedUrl())
                    .build());
        }

        return StudyMeetingSummaryResponseDto.builder()
                .id(vo.id())
                .title(vo.title())
                .participantNumber(vo.participantNumber())
                .creatorName(vo.creatorName())
                .isEditable(vo.isEditable())
                .links(links)
                .build();
    }

    /**
     * StudyMeetingSummaryVo 리스트를 StudyMeetingSummaryResponseDto 리스트로 변환
     */
    public List<StudyMeetingSummaryResponseDto> toStudyMeetingSummaryResponseDtoList(List<StudyMeetingSummaryVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toStudyMeetingSummaryResponseDto)
                .toList();
    }

    /**
     * StudyMeetingPageResultVo를 StudyMeetingSummaryResponseDto 리스트로 변환
     * 페이지 결과에서 실제 콘텐츠만 추출하여 DTO 리스트로 변환
     */
    public List<StudyMeetingSummaryResponseDto> toStudyMeetingSummaryResponseDtoList(StudyMeetingPageResultVo pageResultVo) {
        if (pageResultVo == null || pageResultVo.meetings() == null || pageResultVo.meetings().getContent().isEmpty()) {
            return Collections.emptyList();
        }

        return pageResultVo.meetings().getContent().stream()
                .map(this::toStudyMeetingSummaryResponseDto)
                .toList();
    }

    /**
     * StudySummaryVo 리스트를 StudySummaryResponseDto 리스트로 변환
     */
    public List<StudySummaryResponseDto> toStudySummaryResponseDtoList(List<StudySummaryVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toStudySummaryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * StudySummaryVo Page를 StudySummaryResponseDto Page로 변환
     */
    public Page<StudySummaryResponseDto> toStudySummaryResponseDtoPage(Page<StudySummaryVo> voPage) {
        if (voPage == null) {
            return Page.empty();
        }

        List<StudySummaryResponseDto> dtoList = toStudySummaryResponseDtoList(voPage.getContent());
        // Normalize Pageable to avoid Unpaged serialization issues
        var pageable = voPage.getPageable().isPaged() ? voPage.getPageable() : org.springframework.data.domain.PageRequest.of(0, dtoList.size() == 0 ? 1 : dtoList.size());
        return new PageImpl<>(dtoList, pageable, voPage.getTotalElements());
    }

    /**
     * StudyParticipantCreatedVo → StudyJoinResponseDto 변환
     */
    public StudyJoinResponseDto toStudyJoinResponseDto(StudyParticipantCreatedVo vo) {
        return StudyJoinResponseDto.builder()
                .participantId(vo.id())
                .studyId(vo.studyId())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .build();
    }

    /**
     * 테스트용 링크 목록 생성
     */
    private List<StudyMeetingSummaryResponseDto.Link> createMockLinks(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        
        return IntStream.range(0, count)
                .mapToObj(i -> StudyMeetingSummaryResponseDto.Link.builder()
                        .title("회의록 첨부 링크 " + (i + 1))
                        .url("https://example.com/meeting-notes-" + (i + 1) + ".pdf")
                        .build())
                .toList();
    }

    /**
     * StudyParticipantStatusUpdatedVo → StudyParticipantStatusUpdateResponseDto 변환
     */
    public StudyParticipantStatusUpdateResponseDto toStudyParticipantStatusUpdateResponseDto(
            StudyParticipantStatusUpdatedVo vo) {

        String message = switch (vo.currentStatus()) {
            case APPROVED -> "프로젝트 참가가 승인되었습니다.";
            case REJECTED -> "프로젝트 참가가 거절되었습니다.";
            case CANCELLED -> "프로젝트 참가 신청이 취소되었습니다.";
            default -> "프로젝트 참가 상태가 변경되었습니다.";
        };

        return StudyParticipantStatusUpdateResponseDto.builder()
                .participantId(vo.id())
                .studyId(vo.studyId())
                .memberId(vo.memberId())
                .previousStatus(vo.previousStatus())
                .currentStatus(vo.currentStatus())
                .message(message)
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * StudyParticipantSummaryVo → StudyParticipantSummaryResponseDto 변환
     */
    public StudyParticipantSummaryResponseDto toStudyParticipantSummaryResponseDto(
            StudyParticipantSummaryVo vo) {
        return StudyParticipantSummaryResponseDto.builder()
                .id(vo.id())
                .memberId(vo.memberId())
                .memberName(vo.memberName())
                .memberGrade(vo.memberGrade())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .build();
    }

    /**
     * StudyParticipantVo → StudyParticipantDetailResponseDto 변환
     */
    public StudyParticipantDetailResponseDto toStudyParticipantDetailResponseDto(
            StudyParticipantVo vo, String studyTitle) {
        return StudyParticipantDetailResponseDto.builder()
                .id(vo.id())
                .studyId(vo.studyId())
                .studyTitle(studyTitle)
                .memberId(vo.memberId())
                .memberName(vo.memberName())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * StudyParticipantSummaryVo List → StudyParticipantSummaryResponseDto List 변환
     */
    public List<StudyParticipantSummaryResponseDto> toStudyParticipantSummaryResponseDtoList(
            List<StudyParticipantSummaryVo> voList) {
        return voList.stream()
                .map(this::toStudyParticipantSummaryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * StudyParticipantVo List → StudyParticipantSummaryResponseDto List 변환
     */
    public List<StudyParticipantSummaryResponseDto> toStudyParticipantSummaryResponseDtoListFromVo(
            List<StudyParticipantVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }

        return voList.stream()
                .map(vo -> StudyParticipantSummaryResponseDto.builder()
                        .id(vo.id())
                        .memberId(vo.memberId())
                        .memberName(vo.memberName())
                        .status(vo.status())
                        .createdAt(vo.createdAt())
                        .build())
                .toList();
    }

    /**
     * Page<StudyParticipantSummaryVo> → Page<StudyParticipantSummaryResponseDto> 변환
     */
    public Page<StudyParticipantSummaryResponseDto> toStudyParticipantSummaryResponseDtoPage(
            Page<StudyParticipantSummaryVo> voPage) {
        List<StudyParticipantSummaryResponseDto> dtoList = toStudyParticipantSummaryResponseDtoList(
                voPage.getContent());
        return new PageImpl<>(dtoList, voPage.getPageable(), voPage.getTotalElements());
    }

    /**
     * 프로젝트 참가자 통계 생성
     */
    public StudyParticipantStatsResponseDto toStudyParticipantStatsResponseDto(
            Long studyId, Long approvedCount, Long pendingCount, Integer maxParticipants) {
        boolean isFull = maxParticipants != null && approvedCount >= maxParticipants;

        return StudyParticipantStatsResponseDto.builder()
                .studyId(studyId)
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .maxParticipants(maxParticipants)
                .isFull(isFull)
                .build();
    }

    /**
     * StudyParticipantStatusUpdatedVo → AdminStudyParticipantApprovalResponseDto 변환 (실데이터 버전)
     */
    public AdminStudyParticipantApprovalResponseDto toAdminStudyParticipantApprovalResponseDto(
            StudyParticipantStatusUpdatedVo vo,
            String studyTitle,
            String memberName,
            org.certis.studyplatform.study.domain.StudyParticipantStatus status,
            Long adminId,
            String adminName,
            String reason
    ) {
        return AdminStudyParticipantApprovalResponseDto.builder()
                .participantId(vo.id())
                .studyId(vo.studyId())
                .studyTitle(studyTitle)
                .memberId(vo.memberId())
                .memberName(memberName)
                .status(status)
                .reason(reason)
                .adminId(adminId)
                .adminName(adminName)
                .processedAt(vo.updatedAt())
                .build();
    }
}