package org.certis.studyplatform.study.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.vo.StudyMeetingPageResultVo;
import org.certis.studyplatform.study.domain.vo.StudySummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.presentation.dto.request.*;
import org.certis.studyplatform.study.presentation.dto.response.StudyDetailResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingSummaryResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudySummaryResponseDto;
import org.certis.studyplatform.study.application.command.StudyCommandService;
import org.certis.studyplatform.study.application.mapper.StudyApplicationCommandMapper;
import org.certis.studyplatform.study.application.mapper.StudyApplicationDtoMapper;
import org.certis.studyplatform.study.application.mapper.StudyApplicationQueryMapper;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.certis.studyplatform.study.application.object.query.GetAllStudyMeetingsQuery;
import org.certis.studyplatform.study.application.object.query.GetAllStudiesQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.application.object.query.SearchStudiesQuery;
import org.certis.studyplatform.study.application.query.StudyMeetingQueryService;
import org.certis.studyplatform.study.application.query.StudyQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.domain.service.StudyDomainService;

import org.certis.studyplatform.study.presentation.dto.response.AdminStudyEndSubmissionResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyAttachedResponseDto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Study Facade Service
 *
 * Clean Architecture Application Layer
 * 통합 진입점 - Command와 Query Service 조합
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyFacadeService {

    private final StudyCommandService studyCommandService;
    private final StudyQueryService studyQueryService;
    private final StudyApplicationCommandMapper commandMapper;
    private final StudyApplicationQueryMapper queryMapper;
    private final StudyApplicationDtoMapper dtoMapper;
    private final StudyDomainService studyDomainService;

    @Qualifier("virtualThreadTaskExecutor")
    private final Executor virtualThreadExecutor;
    private final StudyMeetingQueryService studyMeetingQueryService;
    private final S3FileService s3FileService;

    // ================================================================
    // COMMAND OPERATIONS - 상태 변경 작업
    // ================================================================

    /**
     * 프로젝트 생성 (임시 - Spring Security 미구축 상태)
     */
    public void createStudy(StudyCreateRequestDto requestDto, Long creatorId) {
        log.info("Facade: Creating study - {}", requestDto.getTitle());

        // DTO → Command Object 변환
        CreateStudyCommand command = commandMapper.toCreateStudyCommand(requestDto, creatorId);

        // Command Service 호출 (VO 반환)
        StudyVo createdVo = studyCommandService.createStudy(command);

        log.info("Facade: Study created successfully - ID: {}", createdVo.id());
    }

    /**
     * 프로젝트 수정 (임시 - Spring Security 미구축 상태)
     */
    public void updateStudy(StudyUpdateRequestDto requestDto, Long requesterId) {
        log.info("Facade: Updating study - ID: {}", requestDto.getStudyId());

        // DTO → Command Object 변환
        UpdateStudyCommand command = commandMapper.toUpdateStudyCommand(requestDto, requesterId);

        // Command Service 호출 (VO 반환)
        StudyVo updatedVo = studyCommandService.updateStudy(command);

        log.info("Facade: Study updated successfully - ID: {}", updatedVo.id());
    }

    /**
     * 스터디 첨부파일 업로드
     */
    public String uploadStudyAttachment(Long studyId, Long memberId, MultipartFile file) {
        log.info("Facade: Uploading study attachment for study ID: {}, member ID: {}", studyId, memberId);

        // S3에 첨부파일 업로드
        String attachmentUrl = studyCommandService.uploadStudyAttachment(studyId, memberId, file);

        log.info("Facade: Study attachment uploaded successfully for study ID: {}, member ID: {}, URL: {}", studyId, memberId, attachmentUrl);
        return attachmentUrl;
    }

    /**
     * 프로젝트 삭제 (임시 - Spring Security 미구축 상태)
     */
    public void deleteStudy(StudyDeleteRequestDto requestDto, Long requesterId) {
        log.info("Facade: Deleting study - ID: {} by requester: {}", requestDto.getStudyId(),requesterId);

        // DTO → Command Object 변환
        DeleteStudyCommand command = commandMapper.toDeleteStudyCommand(requestDto.getStudyId(), requesterId);

        // Command Service 호출 (void 반환)
        studyCommandService.deleteStudy(command);

        log.info("Facade: Study deleted successfully - ID: {}", requestDto.getStudyId());
    }

    // ================================================================
    // QUERY OPERATIONS - 조회 작업
    // ================================================================

    /**
     * 프로젝트 상세 조회 (DTO 기반)
     */
    public StudyDetailResponseDto getStudyDetail(StudyDetailRequestDto requestDto) {
        log.info("Facade: Getting study detail - ID: {}", requestDto.getStudyId());

        Long studyId = requestDto.getStudyId();

        // 가상 스레드 Executor 사용
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. 프로젝트 기본 정보 조회
            CompletableFuture<StudyVo> studyFuture = CompletableFuture
                    .supplyAsync(() -> {
                        GetStudyByIdQuery query = queryMapper.toGetStudyByIdQuery(studyId);
                        return studyQueryService.getStudyById(query);
                    }, executor);

            // 2. 프로젝트 회의록 목록 조회
            CompletableFuture<StudyMeetingPageResultVo> meetingSummariesFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // GetAllStudyMeetingsQuery 객체 생성
                        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE); // 전체 조회
                        GetAllStudyMeetingsQuery query = new GetAllStudyMeetingsQuery(studyId, pageable);

                        return studyMeetingQueryService.getAllStudyMeetings(query);
                    }, executor);

            // 3. 모든 비동기 작업 완료 대기 및 결과 조합
            CompletableFuture<StudyDetailResponseDto> resultFuture = studyFuture
                    .thenCombine(meetingSummariesFuture, (studyVo, meetingSummaries) -> {
                        // 프로젝트 VO → DTO 변환
                        StudyDetailResponseDto responseDto = dtoMapper.toStudyDetailResponseDto(studyVo);

                        // 회의록 목록을 DTO 리스트로 변환
                        List<StudyMeetingSummaryResponseDto> meetingSummaryDtos =
                                dtoMapper.toStudyMeetingSummaryResponseDtoList(meetingSummaries);

                        return responseDto.toBuilder()
                                .meetingSummaries(meetingSummaryDtos)
                                .build();
                    });

            // 최종 결과 반환
            StudyDetailResponseDto result = resultFuture.join(); // 예외 발생 시 전역 핸들러로 전파됨

            log.info("Facade: Study detail retrieved successfully - ID: {}, meetingSummaries: {}",
                    result.getId(),
                    result.getMeetingSummaries() != null ? result.getMeetingSummaries().size() : 0);

            return result;
        }
    }

    /**
     * 전체 프로젝트 조회 (ResponseDTO 반환)
     */
    public Page<StudySummaryResponseDto> getAllStudies(Pageable pageable) {
        log.info("Facade: Getting all studies with pagination");

        // DTO → Query Object 변환
        GetAllStudiesQuery query = queryMapper.toGetAllStudiesQuery(pageable);

        // Query Service 호출 (VO 반환)
        Page<StudySummaryVo> studies = studyQueryService.getAllStudies(query);

        // VO → DTO 변환 (FacadeService에서만 수행)
        Page<StudySummaryResponseDto> responseDto = dtoMapper.toStudySummaryResponseDtoPage(studies);

        log.info("Facade: All studies retrieved - found {} studies", responseDto.getTotalElements());
        return responseDto;
    }


    /**
     * 통합 고급 검색으로 프로젝트 검색 (5가지 필터 지원)
     * 
     * @param requestDto 고급 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 프로젝트 목록
     */
    public Page<StudySummaryResponseDto> searchStudiesAdvanced(
            StudyAdvancedSearchRequestDto requestDto, Pageable pageable) {
        log.info("Facade: Advanced searching studies - keyword: {}, category: {}, subcategory: {}, semester: {}, status: {}",
                requestDto.getKeyword(),requestDto.getCategory(),
                requestDto.getSubcategory(), requestDto.getSemester(), requestDto.getStatus());

        // DTO → Query Object 변환 (CPU-bound 작업이므로 비동기 처리 불필요)
        SearchStudiesQuery query = queryMapper.toSearchStudiesQuery(requestDto, pageable);

        // 가상 스레드 Executor를 사용하여 I/O-bound 작업을 비동기로 처리
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Page<StudySummaryResponseDto>> searchFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // Query Service 호출 (I/O-bound 작업)
                        return studyQueryService.searchStudies(query);
                    }, executor)
                    .thenApply(studiesVo -> {
                        // VO → DTO 변환 (CPU-bound 작업)
                        return dtoMapper.toStudySummaryResponseDtoPage(studiesVo);
                    });

            // 비동기 작업 완료 대기 및 결과 반환
            Page<StudySummaryResponseDto> responseDto = searchFuture.join();

            log.info("Facade: Advanced search completed - found {} studies", responseDto.getTotalElements());
            return responseDto;
        }
    }

    // ================================================================
    // ADDITIONAL QUERY METHODS - 첨부파일/회의록 조회
    // ================================================================

    /**
     * 프로젝트 회의록 요약 목록 조회 (DTO 반환)
     */
    public List<StudyMeetingSummaryResponseDto> getStudyMeetings(Long studyId) {
        log.info("Facade: Getting meetings for study - ID: {}", studyId);

        // Query meetings for the given study and map to DTOs
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        GetAllStudyMeetingsQuery query = new GetAllStudyMeetingsQuery(studyId, pageable);
        StudyMeetingPageResultVo meetingPageResult = studyMeetingQueryService.getAllStudyMeetings(query);

        List<StudyMeetingSummaryResponseDto> meetingSummaries =
                dtoMapper.toStudyMeetingSummaryResponseDtoList(meetingPageResult);

        log.info("Facade: Found {} meetings for study - ID: {}", meetingSummaries.size(), studyId);
        return meetingSummaries;
    }

    /**
     * 스터디 종료 (DTO 반환)
     */
    public StudyDetailResponseDto endStudy(StudyEndRequestDto requestDto, Long requesterId) {
        log.info("Facade: Ending study - ID: {}, requesterId: {}", requestDto.getStudyId(), requesterId);

        // Command 객체 생성
        String attachmentUrl = requestDto.getAttachment() != null ? requestDto.getAttachment().getAttachedUrl() : null;
        EndStudyCommand command = EndStudyCommand.of(requestDto.getStudyId(), requesterId, attachmentUrl);

        // Command Service 호출 (VO 반환)
        studyCommandService.endStudy(command);

        // 상태 확정 후 최신 데이터로 재조회하여 DTO 변환
        GetStudyByIdQuery refreshQuery = queryMapper.toGetStudyByIdQuery(requestDto.getStudyId());
        StudyVo refreshed = studyQueryService.getStudyById(refreshQuery);
        StudyDetailResponseDto responseDto = dtoMapper.toStudyDetailResponseDto(refreshed);

        log.info("Facade: Study ended successfully - ID: {}", refreshed.id());
        return responseDto;
    }

    /**
     * Admin: 스터디 종료 제출 조회 (S3 메타 포함)
     */
    public AdminStudyEndSubmissionResponseDto getAdminStudyEndSubmission(Long studyId) {
        var infoVo = studyDomainService.getEndSubmissionInfo(studyId);
        StudyAttachedResponseDto attachment = null;
        if (infoVo.attachmentUrl() != null) {
            var info = s3FileService.getObjectInfo(infoVo.attachmentUrl());
            if (info != null) {
                attachment = StudyAttachedResponseDto.builder()
                        .id(null)
                        .name(info.getName())
                        .type(info.getContentType())
                        .size(info.getSize() != null ? String.valueOf(info.getSize()) : null)
                        .attachedUrl(info.getUrl())
                        .build();
            }
        }
        return AdminStudyEndSubmissionResponseDto.builder()
                .studyId(studyId)
                .status(infoVo.status())
                .submittedAt(infoVo.submittedAt())
                .attachment(attachment)
                .category(infoVo.category())
                .subCategory(infoVo.subCategory())
                .title(infoVo.title())
                .description(infoVo.description())
                .creatorId(infoVo.creatorId())
                .startedAt(infoVo.startedAt())
                .endedAt(infoVo.endedAt())
                .currentParticipantNumber(infoVo.currentParticipantNumber())
                .maxParticipantNumber(infoVo.maxParticipantNumber())
                .build();
    }

    public java.util.List<AdminStudyEndSubmissionResponseDto> getAdminStudyEndSubmissionsInProgress() {
        var list = studyDomainService.getEndSubmissionsInProgress();
        java.util.List<AdminStudyEndSubmissionResponseDto> result = new java.util.ArrayList<>();
        for (var infoVo : list) {
            StudyAttachedResponseDto attachment = null;
            if (infoVo.attachmentUrl() != null) {
                var info = s3FileService.getObjectInfo(infoVo.attachmentUrl());
                if (info != null) {
                    attachment = StudyAttachedResponseDto.builder()
                            .id(null)
                            .name(info.getName())
                            .type(info.getContentType())
                            .size(info.getSize() != null ? String.valueOf(info.getSize()) : null)
                            .attachedUrl(info.getUrl())
                            .build();
                }
            }
            result.add(AdminStudyEndSubmissionResponseDto.builder()
                    .studyId(infoVo.studyId())
                    .status(infoVo.status())
                    .submittedAt(infoVo.submittedAt())
                    .attachment(attachment)
                    .category(infoVo.category())
                    .subCategory(infoVo.subCategory())
                    .title(infoVo.title())
                    .description(infoVo.description())
                    .creatorId(infoVo.creatorId())
                    .startedAt(infoVo.startedAt())
                    .endedAt(infoVo.endedAt())
                    .currentParticipantNumber(infoVo.currentParticipantNumber())
                    .maxParticipantNumber(infoVo.maxParticipantNumber())
                    .build());
        }
        return result;
    }

    // ================================================================
    // ADMIN COMMAND OPERATIONS (delegate to command service)
    // ================================================================

    public void approveStudyEnd(Long studyId, Long adminId) {
        studyCommandService.approveStudyEnd(studyId, adminId);
    }

    public void rejectStudyEnd(Long studyId, Long adminId) {
        studyCommandService.rejectStudyEnd(studyId, adminId);
    }

    public void approveStudyCreation(Long studyId, Long adminId) {
        studyCommandService.approveStudyCreation(studyId, adminId);
    }

    public void rejectStudyCreation(Long studyId, Long adminId) {
        studyCommandService.rejectStudyCreation(studyId, adminId);
    }


}