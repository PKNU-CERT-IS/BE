package org.certis.studyplatform.study.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.study.application.StudyFacadeService;
import org.certis.studyplatform.study.presentation.dto.request.StudyCreateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyDeleteRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyDetailRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyUpdateRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyAdvancedSearchRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyDetailResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudySummaryResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingSummaryResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.certis.studyplatform.study.presentation.dto.request.StudyEndRequestDto;

import java.util.List;

/**
 * Study REST Controller
 *
 * Clean Architecture Presentation Layer
 * 이미지의 API 명세에 따른 프로젝트 관련 엔드포인트 제공
 *
 * Spring Security 미구축 상태에 맞춰 임시로 leaderId를 파라미터로 받음
 * @ModelAttribute를 사용한 커스텀 DTO 파라미터 바인딩
 *
 * API 명세:
 * - POST /api/v1/study/create - 프로젝트 생성
 * - PUT /api/v1/study/update - 프로젝트 정보 수정
 * - DELETE /api/v1/study/delete - 프로젝트 정보 삭제
 * - GET /api/v1/study/detail - 프로젝트 세부 정보 조회
 * - GET /api/v1/study/search - 프로젝트 검색
 * - GET /api/v1/study/search/keyword - 통합 고급 검색 (5가지 필터 지원)
 * - POST /api/v1/study/meeting/create - 프로젝트 회의록 생성
 * - GET /api/v1/study/meeting/detail - 프로젝트 회의록 상세 조회
 * - PUT /api/v1/study/meeting/edit - 프로젝트 회의록 수정
 * - DELETE /api/v1/study/meeting/delete - 프로젝트 회의록 삭제
 */
@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@Slf4j
public class StudyController {

    private final StudyFacadeService studyFacadeService;

    /**
     * 프로젝트 생성
     *
     * @param request 프로젝트 생성 요청 DTO (leaderId, creatorName 포함)
     * @return 생성된 프로젝트 정보
     */
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createStudy(
            @Valid @RequestBody StudyCreateRequestDto request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser
            ) {
        log.info("REST: Creating study - {}", request.getTitle());

        // Facade Service 호출
        studyFacadeService.createStudy(request, currentUser.getId(), idempotencyKey);

        log.info("REST: Study created successfully");

        return GlobalResponseHandler.success(ResponseStatus.STUDY_CREATE_SUCCESS);
    }

    /**
     * 프로젝트 정보 수정
     *
     * @param request 프로젝트 수정 요청 DTO (studyId, requesterId 포함)
     * @return 수정된 프로젝트 정보
     */
    @PutMapping("/update")
    public ResponseEntity<GlobalResponseHandler<Void>> updateStudy(
            @Valid @RequestBody StudyUpdateRequestDto request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Updating study - ID: {}", request.getStudyId());

        // Facade Service 호출
        studyFacadeService.updateStudy(request,currentUser.getId(), idempotencyKey);

        return GlobalResponseHandler.success(ResponseStatus.STUDY_UPDATE_SUCCESS);
    }

    /**
     * 프로젝트 정보 삭제
     *
     * @param request 프로젝트 삭제 요청 DTO (studyId, requesterId 포함)
     * @return 성공 응답
     */
    @DeleteMapping("/delete")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteStudy(
            @Valid @RequestBody StudyDeleteRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Deleting study - ID: {} by requester: {}", request.getStudyId());

        // Facade Service 호출
        studyFacadeService.deleteStudy(request,currentUser.getId());

        log.info("REST: Study deleted successfully - ID: {}", request.getStudyId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_DELETE_SUCCESS);
    }

    /**
     * 프로젝트 세부 정보 조회 (@ModelAttribute 사용)
     *
     * @param request 프로젝트 상세 조회 요청 DTO
     * @return 프로젝트 상세 정보
     */
    @GetMapping("/detail")
    public ResponseEntity<GlobalResponseHandler<StudyDetailResponseDto>> getStudyDetail(
            @Valid @ModelAttribute StudyDetailRequestDto request) {
        log.info("REST: Getting study detail - ID: {}", request.getStudyId());

        // Facade Service 호출
        StudyDetailResponseDto studyDetail = studyFacadeService.getStudyDetail(request);

        log.info("REST: Study detail retrieved successfully - ID: {}", studyDetail.getId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_FIND_SUCCESS, studyDetail);
    }

    /**
     * 프로젝트 통합 검색 (@ModelAttribute 사용)
     * 5가지 필터 조건을 지원하는 통합 고급 검색
     * - keyword: 제목, 설명, 생성자명 포함 검색
     * - semester: 학기 기간 필터 (예: "2025-01", "2024-02")
     * - category: 카테고리 필터
     * - subcategory: 서브카테고리 필터
     * - status: 프로젝트 상태 필터 (Ready, InProgress, Completed)
     *
     * @param searchRequest 통합 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 프로젝트 목록
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<Page<StudySummaryResponseDto>>> searchStudiesByKeyword(
            @Valid @ModelAttribute StudyAdvancedSearchRequestDto searchRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Unified study search - keyword: {}, category: {}, subcategory: {}, semester: {}, studyStatus: {}, page: {}, size: {}",
                searchRequest.getKeyword(),  searchRequest.getCategory(),
                searchRequest.getSubcategory(), searchRequest.getSemester(), searchRequest.getStudyStatus(),
                pageable.getPageNumber(), pageable.getPageSize());

        // 통합 고급 검색 Facade Service 호출
        Page<StudySummaryResponseDto> result = studyFacadeService.searchStudiesAdvanced(searchRequest, pageable);

        log.info("REST: Unified search completed - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_SEARCH_SUCCESS, result);
    }

    /**
     * 전체 프로젝트 조회
     *
     * @param pageable 페이징 정보
     * @return 전체 프로젝트 목록
     */
    @GetMapping
    public ResponseEntity<GlobalResponseHandler<Page<StudySummaryResponseDto>>> getAllStudies(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST: Getting all studies - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Facade Service 호출
        Page<StudySummaryResponseDto> result = studyFacadeService.getAllStudies(pageable);

        log.info("REST: All studies retrieved - found {} results", result.getTotalElements());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_SEARCH_SUCCESS, result);
    }

    /**
     * 프로젝트 회의록 요약 목록 조회
     *
     * @param studyId 프로젝트 ID
     * @return 회의록 요약 목록
     */
    @GetMapping("/{studyId}/meetings")
    public ResponseEntity<GlobalResponseHandler<List<StudyMeetingSummaryResponseDto>>> getStudyMeetings(
            @PathVariable Long studyId) {
        log.info("REST: Getting meetings for study - ID: {}", studyId);

        // Facade Service 호출 (VO → DTO 변환 포함)
        List<StudyMeetingSummaryResponseDto> meetings = studyFacadeService.getStudyMeetings(studyId);

        log.info("REST: Found {} meetings for study - ID: {}", meetings.size(), studyId);

        return GlobalResponseHandler.success(ResponseStatus.STUDY_FIND_SUCCESS, meetings);
    }

    /**
     * 스터디 종료
     * POST /api/v1/study/end
     */
    @PostMapping(value = "/end")
    public ResponseEntity<GlobalResponseHandler<StudyDetailResponseDto>> endStudy(
            @Valid @RequestBody StudyEndRequestDto requestDto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentUser currentUser) {
        log.info("REST: Ending study - ID: {}, requesterId: {}", requestDto.getStudyId(), currentUser.getId());

        // Facade Service 호출 (VO → DTO 변환 포함)
        StudyDetailResponseDto endedStudy = studyFacadeService.endStudy(
                requestDto,
            currentUser.getId(),
            idempotencyKey
        );

        log.info("REST: Study ended successfully - ID: {}", endedStudy.getId());

        return GlobalResponseHandler.success(ResponseStatus.STUDY_END_SUCCESS, endedStudy);
    }
}
