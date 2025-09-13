package org.certis.studyplatform.study.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.study.application.StudyMeetingFacadeService;
import org.certis.studyplatform.study.presentation.dto.request.StudyMeetingCreateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyMeetingDeleteRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyMeetingDetailRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyMeetingUpdateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyMeetingAllRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingDetailResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingSummaryResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Study Meeting REST Controller
 *
 * Clean Architecture Presentation Layer
 * 이미지의 API 명세에 따른 프로젝트 회의록 관련 엔드포인트 제공
 *
 * Spring Security 미구축 상태에 맞춰 임시로 leaderId를 파라미터로 받음
 * @ModelAttribute를 사용한 커스텀 DTO 파라미터 바인딩
 *
 * API 명세:
 * - POST /api/v1/study/meeting/create - 프로젝트 회의록 생성
 * - GET /api/v1/study/meeting/detail - 프로젝트 회의록 상세 조회
 * - PUT /api/v1/study/meeting/edit - 프로젝트 회의록 수정
 * - DELETE /api/v1/study/meeting/delete - 프로젝트 회의록 삭제
 * - GET /api/v1/study/meeting/all - 프로젝트 회의록 전체 목록 조회
 */
@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingController {
    // =================================================================
    // STUDY MEETING ENDPOINTS (/api/v1/study/meeting/*)
    // =================================================================

    private final StudyMeetingFacadeService studyMeetingFacadeService;

    /**
     * 프로젝트 회의록 생성
     *
     * @param request 회의록 생성 요청 DTO
     * @return 성공 응답
     */
    @PostMapping("/meeting/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createStudyMeeting(
            @Valid @RequestBody StudyMeetingCreateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Creating study meeting - studyId: {}, title: {}", request.getStudyId(), request.getTitle());

        // Facade Service 호출
        studyMeetingFacadeService.createStudyMeeting(request,currentUser.getId());

        log.info("REST: Study meeting created successfully");

        return GlobalResponseHandler.success(ResponseStatus.STUDY_MEETING_CREATE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 상세 조회
     *
     * @param request 회의록 상세 조회 요청 DTO
     * @return 회의록 상세 정보
     */
    @GetMapping("/meeting/detail")
    public ResponseEntity<GlobalResponseHandler<StudyMeetingDetailResponseDto>> getStudyMeetingDetail(
            @Valid @ModelAttribute StudyMeetingDetailRequestDto request) {
        log.info("REST: Getting study meeting detail - meetingId: {}", request.getMeetingId());

        // Facade Service 호출
        StudyMeetingDetailResponseDto response = studyMeetingFacadeService.getStudyMeetingDetail(request);

        log.info("REST: Study meeting detail retrieved successfully - ID: {}", response.getId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_MEETING_FIND_SUCCESS, response);
    }

    /**
     * 프로젝트 회의록 수정
     *
     * @param request 회의록 수정 요청 DTO
     * @return 성공 응답
     */
    @PutMapping("/meeting/edit")
    public ResponseEntity<GlobalResponseHandler<Void>> updateStudyMeeting(
            @Valid @RequestBody StudyMeetingUpdateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {
        log.info("REST: Updating study meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(),currentUser.getId());

        log.warn("CurrentUser debug: {}", currentUser);

        // Facade Service 호출
        studyMeetingFacadeService.updateStudyMeeting(request,currentUser.getId());

        log.info("REST: Study meeting updated successfully");

        return GlobalResponseHandler.success(ResponseStatus.STUDY_MEETING_UPDATE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 삭제
     *
     * @param request 회의록 삭제 요청 DTO
     * @return 성공 응답
     */
    @DeleteMapping("/meeting/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteStudyMeeting(
            @Valid @RequestBody StudyMeetingDeleteRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Deleting study meeting - meetingId: {}, requesterId: {}",
                request.getMeetingId(),currentUser.getId());

        // Facade Service 호출
        studyMeetingFacadeService.deleteStudyMeeting(request,currentUser.getId());

        log.info("REST: Study meeting deleted successfully");

        return GlobalResponseHandler.success(ResponseStatus.STUDY_MEETING_DELETE_SUCCESS);
    }

    /**
     * 프로젝트 회의록 전체 목록 조회
     *
     * @param request 회의록 전체 목록 조회 요청 DTO
     * @param pageable 페이징 정보
     * @return 회의록 목록 (페이징)
     */
    @GetMapping("/meeting/all")
    public ResponseEntity<GlobalResponseHandler<Page<StudyMeetingSummaryResponseDto>>> getAllStudyMeetings(
            @Valid @ModelAttribute StudyMeetingAllRequestDto request,
            Pageable pageable) {
        log.info("REST: Getting all study meetings - studyId: {}, page: {}, size: {}",
                request.getStudyId(), pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<StudyMeetingSummaryResponseDto> response = studyMeetingFacadeService.getAllStudyMeetings(request, pageable);

        log.info("REST: Study meetings retrieved successfully - totalElements: {}, totalPages: {}",
                response.getTotalElements(), response.getTotalPages());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_MEETING_FIND_SUCCESS, response);
    }
}
