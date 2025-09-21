package org.certis.studyplatform.project.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.command.ProjectCommandService;
import org.certis.studyplatform.project.application.mapper.ProjectApplicationCommandMapper;
import org.certis.studyplatform.project.application.mapper.ProjectApplicationQueryMapper;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectCommand;
import org.certis.studyplatform.project.application.object.command.EndProjectCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectCommand;
import org.certis.studyplatform.project.application.object.query.GetAllProjectMeetingsQuery;
import org.certis.studyplatform.project.application.object.query.GetAllProjectsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.application.object.query.SearchProjectsQuery;
import org.certis.studyplatform.project.application.query.ProjectMeetingQueryService;
import org.certis.studyplatform.project.application.query.ProjectQueryService;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingPageResultVo;
import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.presentation.dto.request.ProjectCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectDeleteRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectSearchRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectUpdateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectAdvancedSearchRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectSummaryResponseDto;
import org.certis.studyplatform.project.application.mapper.ProjectApplicationDtoMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.certis.studyplatform.project.presentation.dto.response.ProjectAttachedResponseDto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Project Facade Service
 *
 * Clean Architecture Application Layer
 * 통합 진입점 - Command와 Query Service 조합
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectFacadeService {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;
    private final ProjectApplicationCommandMapper commandMapper;
    private final ProjectApplicationQueryMapper queryMapper;
    private final ProjectApplicationDtoMapper dtoMapper;

    @Qualifier("virtualThreadTaskExecutor")
    private final Executor virtualThreadExecutor;
    private final ProjectMeetingQueryService projectMeetingQueryService;

    // ================================================================
    // COMMAND OPERATIONS - 상태 변경 작업
    // ================================================================

    /**
     * 프로젝트 생성 (임시 - Spring Security 미구축 상태)
     */
    public void createProject(ProjectCreateRequestDto requestDto, Long creatorId) {
        log.info("Facade: Creating project - {}", requestDto.getTitle());

        // DTO → Command Object 변환
        CreateProjectCommand command = commandMapper.toCreateProjectCommand(requestDto, creatorId);

        // Command Service 호출 (VO 반환)
        ProjectVo createdVo = projectCommandService.createProject(command);

        log.info("Facade: Project created successfully - ID: {}", createdVo.id());
    }

    /**
     * 프로젝트 수정 (임시 - Spring Security 미구축 상태)
     */
    public void updateProject(ProjectUpdateRequestDto requestDto, Long requesterId) {
        log.info("Facade: Updating project - ID: {}", requestDto.getProjectId());

        // DTO → Command Object 변환
        UpdateProjectCommand command = commandMapper.toUpdateProjectCommand(requestDto, requesterId);

        // Command Service 호출 (VO 반환)
        ProjectVo updatedVo = projectCommandService.updateProject(command);

        log.info("Facade: Project updated successfully - ID: {}", updatedVo.id());
    }

    /**
     * 프로젝트 삭제 (임시 - Spring Security 미구축 상태)
     */
    public void deleteProject(ProjectDeleteRequestDto requestDto, Long requesterId) {
        log.info("Facade: Deleting project - ID: {} by requester: {}", requestDto.getProjectId(), requesterId);

        // DTO → Command Object 변환
        DeleteProjectCommand command = commandMapper.toDeleteProjectCommand(requestDto.getProjectId(),requesterId);

        // Command Service 호출 (void 반환)
        projectCommandService.deleteProject(command);

        log.info("Facade: Project deleted successfully - ID: {}", requestDto.getProjectId());
    }

    // ================================================================
    // QUERY OPERATIONS - 조회 작업
    // ================================================================

    /**
     * 프로젝트 상세 조회 (DTO 기반)
     */
    public ProjectDetailResponseDto getProjectDetail(ProjectDetailRequestDto requestDto) {
        log.info("Facade: Getting project detail - ID: {}", requestDto.getProjectId());

        Long projectId = requestDto.getProjectId();

        // 가상 스레드 Executor 사용
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. 프로젝트 기본 정보 조회
            CompletableFuture<ProjectVo> projectFuture = CompletableFuture
                    .supplyAsync(() -> {
                        GetProjectByIdQuery query = queryMapper.toGetProjectByIdQuery(projectId);
                        return projectQueryService.getProjectById(query);
                    }, executor);

            // 2. 프로젝트 회의록 목록 조회
            CompletableFuture<ProjectMeetingPageResultVo> meetingSummariesFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // GetAllProjectMeetingsQuery 객체 생성
                        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE); // 전체 조회
                        GetAllProjectMeetingsQuery query = new GetAllProjectMeetingsQuery(projectId, pageable);

                        return projectMeetingQueryService.getAllProjectMeetings(query);
                    }, executor);

            // 3. 모든 비동기 작업 완료 대기 및 결과 조합
            CompletableFuture<ProjectDetailResponseDto> resultFuture = projectFuture
                    .thenCombine(meetingSummariesFuture, (projectVo, meetingSummaries) -> {
                        // 프로젝트 VO → DTO 변환
                        ProjectDetailResponseDto responseDto = dtoMapper.toProjectDetailResponseDto(projectVo);

                        // 회의록 목록을 DTO 리스트로 변환
                        List<ProjectMeetingSummaryResponseDto> meetingSummaryDtos =
                                dtoMapper.toProjectMeetingSummaryResponseDtoList(meetingSummaries);

                        return responseDto.toBuilder()
                                .meetingSummaries(meetingSummaryDtos)
                                .build();
                    });

            // 최종 결과 반환
            ProjectDetailResponseDto result = resultFuture.join(); // 예외 발생 시 전역 핸들러로 전파됨

            log.info("Facade: Project detail retrieved successfully - ID: {}, meetingSummaries: {}",
                    result.getId(),
                    result.getMeetingSummaries() != null ? result.getMeetingSummaries().size() : 0);

            return result;
        }
    }

    /**
     * 전체 프로젝트 조회 (ResponseDTO 반환)
     */
    public Page<ProjectSummaryResponseDto> getAllProjects(Pageable pageable) {
        log.info("Facade: Getting all projects with pagination");

        // DTO → Query Object 변환
        GetAllProjectsQuery query = queryMapper.toGetAllProjectsQuery(pageable);

        // Query Service 호출 (VO 반환)
        Page<ProjectSummaryVo> projects = projectQueryService.getAllProjects(query);

        // VO → DTO 변환 (FacadeService에서만 수행)
        Page<ProjectSummaryResponseDto> responseDto = dtoMapper.toProjectSummaryResponseDtoPage(projects);

        log.info("Facade: All projects retrieved - found {} projects", responseDto.getTotalElements());
        return responseDto;
    }

    /**
     * 프로젝트 검색 (ResponseDTO 반환)
     */
    public Page<ProjectSummaryResponseDto> searchProjects(ProjectSearchRequestDto requestDto, Pageable pageable) {
        log.info("Facade: Searching projects - keyword: {}", requestDto.getKeyword());

        // DTO → Query Object 변환
        SearchProjectsQuery query = queryMapper.toSearchProjectsQuery(requestDto, pageable);

        // Query Service 호출 (VO 반환)
        Page<ProjectSummaryVo> projects = projectQueryService.searchProjects(query);

        // VO → DTO 변환 (FacadeService에서만 수행)
        Page<ProjectSummaryResponseDto> responseDto = dtoMapper.toProjectSummaryResponseDtoPage(projects);

        log.info("Facade: Project search completed - found {} projects", responseDto.getTotalElements());
        return responseDto;
    }


    /**
     * 통합 고급 검색으로 프로젝트 검색 (5가지 필터 지원)
     * 
     * @param requestDto 고급 검색 요청 DTO
     * @param pageable 페이징 정보
     * @return 검색된 프로젝트 목록
     */
    public Page<ProjectSummaryResponseDto> searchProjectsAdvanced(
            ProjectAdvancedSearchRequestDto requestDto, Pageable pageable) {
        log.info("Facade: Advanced searching projects - keyword: {}, semester: {}, category: {}, subcategory: {}, status: {}",
                requestDto.getKeyword(), requestDto.getSemester(), requestDto.getCategory(),
                requestDto.getSubcategory(), requestDto.getStatus());

        // DTO → Query Object 변환 (CPU-bound 작업이므로 비동기 처리 불필요)
        SearchProjectsQuery query = queryMapper.toSearchProjectsQuery(requestDto, pageable);

        // 가상 스레드 Executor를 사용하여 I/O-bound 작업을 비동기로 처리
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Page<ProjectSummaryResponseDto>> searchFuture = CompletableFuture
                    .supplyAsync(() -> {
                        // Query Service 호출 (I/O-bound 작업)
                        return projectQueryService.searchProjects(query);
                    }, executor)
                    .thenApply(projectsVo -> {
                        // VO → DTO 변환 (CPU-bound 작업)
                        return dtoMapper.toProjectSummaryResponseDtoPage(projectsVo);
                    });

            // 비동기 작업 완료 대기 및 결과 반환
            Page<ProjectSummaryResponseDto> responseDto = searchFuture.join();

            log.info("Facade: Advanced search completed - found {} projects", responseDto.getTotalElements());
            return responseDto;
        }
    }


    /**
     * 프로젝트 회의록 요약 목록 조회 (DTO 반환)
     */
    public List<ProjectMeetingSummaryResponseDto> getProjectMeetings(Long projectId) {
        log.info("Facade: Getting meetings for project - ID: {}", projectId);

        // TODO: ProjectMeetingFacadeService로 위임하거나 별도 구현 필요
        // 임시로 빈 리스트 반환
        var meetings = List.<ProjectMeetingSummaryResponseDto>of();

        log.info("Facade: Found {} meetings for project - ID: {}", meetings.size(), projectId);
        return meetings;
    }

    /**
     * 프로젝트 종료 (DTO 반환)
     */
    public ProjectDetailResponseDto endProject(Long projectId, Long requesterId, List<MultipartFile> files) {
        log.info("Facade: Ending project - ID: {}, requesterId: {}", projectId, requesterId);

        // Command 객체 생성
        EndProjectCommand command = EndProjectCommand.of(projectId, requesterId, files);

        // Command Service 호출 (VO 반환)
        ProjectVo endedVo = projectCommandService.endProject(command);

        // VO → DTO 변환
        ProjectDetailResponseDto responseDto = dtoMapper.toProjectDetailResponseDto(endedVo);

        log.info("Facade: Project ended successfully - ID: {}", endedVo.id());
        return responseDto;
    }


}