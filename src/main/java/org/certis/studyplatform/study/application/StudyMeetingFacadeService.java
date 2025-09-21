package org.certis.studyplatform.study.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.vo.StudyMeetingCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingDetailVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingPageResultVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingUpdatedVo;
import org.certis.studyplatform.study.presentation.dto.request.*;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingDetailResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingSummaryResponseDto;
import org.certis.studyplatform.study.application.command.StudyMeetingCommandService;
import org.certis.studyplatform.study.application.object.command.CreateStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.query.GetAllStudyMeetingsQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyMeetingByIdQuery;
import org.certis.studyplatform.study.application.query.StudyMeetingQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;



/**
 * Study Meeting Facade Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 관련 기능 전용 Facade Service
 * StudyFacadeService에서 분리하여 독립적으로 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingFacadeService {

    private final StudyMeetingCommandService studyMeetingCommandService;
    private final StudyMeetingQueryService studyMeetingQueryService;

    // ================================================================
    // STUDY MEETING OPERATIONS - 회의록 관리
    // ================================================================

    /**
     * 프로젝트 회의록 생성
     *
     * @param request 회의록 생성 요청 DTO
     */
    public void createStudyMeeting(StudyMeetingCreateRequestDto request, Long writerId) {
        log.info("MeetingFacade: Creating study meeting - studyId: {}, title: {}",
                request.getStudyId(), request.getTitle());
        
        // DTO → Command 변환
        CreateStudyMeetingCommand command = CreateStudyMeetingCommand.of(
            request.getStudyId(),
            writerId,
            request.getTitle(),
            request.getContent(),
            request.getParticipantNumber(),
            request.getLinks()
        );
        
        // Command Service 호출
        StudyMeetingCreatedVo createdVo = studyMeetingCommandService.createStudyMeeting(command);
        
        log.info("MeetingFacade: Study meeting created successfully - ID: {}", createdVo.id());
    }

    /**
     * 프로젝트 회의록 상세 조회
     *
     * @param request 회의록 상세 조회 요청 DTO
     * @return 회의록 상세 정보
     */
    public StudyMeetingDetailResponseDto getStudyMeetingDetail(StudyMeetingDetailRequestDto request) {
        log.info("MeetingFacade: Getting study meeting detail - meetingId: {}", request.getMeetingId());
        
        // DTO → Query 변환
        GetStudyMeetingByIdQuery query = GetStudyMeetingByIdQuery.of(request.getMeetingId());
        
        // Query Service 호출
        StudyMeetingDetailVo meetingVo = studyMeetingQueryService.getStudyMeetingById(query);
        
        // VO → DTO 변환
        StudyMeetingDetailResponseDto responseDto = StudyMeetingDetailResponseDto.builder()
                .id(meetingVo.id())
                .studyId(meetingVo.studyId())
                .title(meetingVo.title())
                .content(meetingVo.content())
                .participantNumber(meetingVo.participantNumber())
                .writerId(meetingVo.writerId())
                .writerName("알 수 없음")
                .createdAt(meetingVo.createdAt())
                .updatedAt(meetingVo.updatedAt())
                .isEditable(meetingVo.isEditable())
                .links(meetingVo.attachedLinks() == null ? Collections.emptyList() : meetingVo.attachedLinks().stream()
                        .map(linkVo -> StudyMeetingDetailResponseDto.Link.builder()
                                .title(linkVo.name())
                                .url(linkVo.attachedUrl())
                                .build())
                        .toList())
                .build();
        
        log.info("MeetingFacade: Study meeting detail retrieved successfully - ID: {}", responseDto.getId());
        return responseDto;
    }

    /**
     * 프로젝트 회의록 수정
     *
     * @param request 회의록 수정 요청 DTO
     */
    public void updateStudyMeeting(StudyMeetingUpdateRequestDto request, Long requesterId) {
        log.info("MeetingFacade: Updating study meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(),requesterId);
        
        // DTO → Command 변환
        UpdateStudyMeetingCommand command = UpdateStudyMeetingCommand.of(
            request.getMeetingId(),
                requesterId,
            request.getTitle(),
            request.getContent(),
            request.getParticipantNumber(),
            request.getLinks()
        );
        
        // Command Service 호출
        StudyMeetingUpdatedVo updatedVo = studyMeetingCommandService.updateStudyMeeting(command);
        
        log.info("MeetingFacade: Study meeting updated successfully - ID: {}", updatedVo.id());
    }

    /**
     * 프로젝트 회의록 삭제
     *
     * @param request 회의록 삭제 요청 DTO
     */
    public void deleteStudyMeeting(StudyMeetingDeleteRequestDto request, Long requesterId) {
        log.info("MeetingFacade: Deleting study meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(),requesterId);
        
        // DTO → Command 변환
        DeleteStudyMeetingCommand command = DeleteStudyMeetingCommand.of(
            request.getMeetingId(),
                requesterId
        );
        
        // Command Service 호출
        studyMeetingCommandService.deleteStudyMeeting(command);
        
        log.info("MeetingFacade: Study meeting deleted successfully");
    }

    /**
     * 프로젝트 회의록 전체 목록 조회 (페이징 지원)
     *
     * @param request 회의록 전체 목록 조회 요청 DTO
     * @param pageable 페이징 정보
     * @return 회의록 목록 (페이징)
     */
    public Page<StudyMeetingSummaryResponseDto> getAllStudyMeetings(StudyMeetingAllRequestDto request, Pageable pageable) {
        log.info("MeetingFacade: Getting all study meetings - studyId: {}, page: {}, size: {}",
                request.getStudyId(), pageable.getPageNumber(), pageable.getPageSize());
        
        // DTO → Query 변환
        GetAllStudyMeetingsQuery query = GetAllStudyMeetingsQuery.of(request.getStudyId(), pageable);
        
        // Query Service 호출
        StudyMeetingPageResultVo meetingVos = studyMeetingQueryService.getAllStudyMeetings(query);
        
        // VO → DTO 변환 (Page.map 사용으로 직접 변환)
        Page<StudyMeetingSummaryResponseDto> result = meetingVos.meetings().map(vo ->
                StudyMeetingSummaryResponseDto.builder()
                        .id(vo.id())
                        .title(vo.title())
                        .participantNumber(vo.participantNumber())
                        .creatorName(vo.creatorName())
                        .createdAt(vo.createdAt())
                        .isEditable(vo.isEditable())
                        .links(vo.hasLinks() ? createMockLinks(vo.safeLinkCount()) : Collections.emptyList())
                        .build());

        log.info("MeetingFacade: Found {} meetings for study - ID: {}", result.getTotalElements(), request.getStudyId());
        
        return result;
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
} 