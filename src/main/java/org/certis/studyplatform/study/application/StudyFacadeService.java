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

    @Qualifier("virtualThreadTaskExecutor")
    private final Executor virtualThreadExecutor;
    private final StudyMeetingQueryService studyMeetingQueryService;

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
        log.info("Facade: Advanced searching studies - keyword: {}, category: {}, subcategory: {}, status: {}",
                requestDto.getKeyword(),requestDto.getCategory(),
                requestDto.getSubcategory(), requestDto.getStatus());

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

        // TODO: StudyMeetingFacadeService로 위임하거나 별도 구현 필요
        // 임시로 빈 리스트 반환
        var meetings = List.<StudyMeetingSummaryResponseDto>of();

        log.info("Facade: Found {} meetings for study - ID: {}", meetings.size(), studyId);
        return meetings;
    }

    /**
     * 스터디 종료 (DTO 반환)
     */
    public StudyDetailResponseDto endStudy(Long studyId, Long requesterId, List<MultipartFile> files) {
        log.info("Facade: Ending study - ID: {}, requesterId: {}", studyId, requesterId);

        // Command 객체 생성
        EndStudyCommand command = EndStudyCommand.of(studyId, requesterId, files);

        // Command Service 호출 (VO 반환)
        StudyVo endedVo = studyCommandService.endStudy(command);

        // VO → DTO 변환
        StudyDetailResponseDto responseDto = dtoMapper.toStudyDetailResponseDto(endedVo);

        log.info("Facade: Study ended successfully - ID: {}", endedVo.id());
        return responseDto;
    }


}