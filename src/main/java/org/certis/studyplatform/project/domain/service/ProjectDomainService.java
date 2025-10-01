package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectCommand;
import org.certis.studyplatform.project.application.object.command.EndProjectCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectCommand;
import org.certis.studyplatform.project.application.object.query.GetAllProjectsQuery;
import org.certis.studyplatform.project.application.object.query.GetCompletedProjectsByMemberQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.application.object.query.SearchProjectsQuery;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchCriteriaVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchResultVo;
import org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

/**
 * Project Domain Service
 *
 * Clean Architecture Domain Layer
 * 프로젝트 비즈니스 로직 처리
 *
 * CQRS 패턴: Command/Query 객체를 받아서 VO를 생성하여 Repository로 전달
 * ✅ ReadModel 제거로 인한 단순화: Repository에서 직접 VO 반환
 * ✅ ProjectVo 내부 검증 로직 활용: 도메인 객체가 자체 유효성을 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectDomainService {

    private final ProjectCommandRepository commandRepository;
    private final ProjectQueryRepository queryRepository;
    private final MemberDomainService memberDomainService;
    private final org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository projectParticipantQueryRepository;
    private final org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository studyParticipantQueryRepository;
    private final org.certis.studyplatform.study.domain.repository.StudyQueryRepository studyQueryRepository;

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 프로젝트 생성
     */
    public ProjectVo createProject(CreateProjectCommand command) {
        log.info("Domain: Creating project from command - {}", command.title());

//         [FIX] Add validation to ensure the creator exists before proceeding.
//        if (!memberQueryRepositoryImpl.existsById(command.creatorId())) {
//            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_NOT_FOUND,
//                    "프로젝트 생성자를 찾을 수 없습니다: " + command.creatorId());
//        }

        // 중복 검사 (Repository 의존성이 필요한 검증만 수행)
//        validateProjectTitleDuplication(command.title());
        // Creation limit enforcement: consider created + joined actives
        enforceCreationLimits(command.creatorId());

        // ProjectVo.createNew() 사용 - 생성 시 자동으로 나머지 검증 수행
        ProjectVo projectVo = ProjectVo.createNew(
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.subCategory(),
                command.startDate(),
                command.endDate(),
                command.creatorId(),
                null, // creatorName은 저장 후 조회 시 설정
                null, // creatorGrade는 저장 후 조회 시 설정
                null, // semester는 아직 구현되지 않음
                null, // status는 아직 구현되지 않음
                command.githubUrl(),
                command.externalUrl(),
                command.demoUrl(),
                command.thumbnailUrl(),
                command.maxParticipants()
        );

        // Command Repository를 통한 저장 (VO 전달)
        ProjectVo savedProjectVo = commandRepository.save(projectVo);

        log.info("Domain: Project created successfully - ID: {}", savedProjectVo.id());

        return savedProjectVo;
    }

    private void enforceCreationLimits(Long creatorId) {
        long activeProjectsJoined = projectParticipantQueryRepository.countActiveProjectsByMemberId(creatorId);

        long activeProjectsCreated = queryRepository.countActiveProjectsCreatedByMemberId(creatorId);

        long activeProjects = activeProjectsJoined + activeProjectsCreated;
        if (activeProjects >= 1) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                    "진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }
    }

    /**
     * 프로젝트 수정
     */
    public ProjectVo updateProject(UpdateProjectCommand command) {
        log.info("Domain: Updating project from command - ID: {}", command.id());

        // 기존 프로젝트 조회 (Repository에서 직접 ProjectVo 반환)
        ProjectVo existingProject = queryRepository.findById(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다: " + command.id()));

        // 권한 검증: STAFF 이상이거나 작성자 본인인지 확인
        validateProjectUpdatePermission(command.requesterId(), existingProject.creatorId());

        // 제목 중복 검사 (자신 제외) - 실제로 제목이 변경되는 경우에만 검사
        if (command.title() != null) {
            String incomingTitle = command.title() != null ? command.title().trim() : null;
            String existingTitle = existingProject.title() != null ? existingProject.title().trim() : null;
            if (incomingTitle != null && (existingTitle == null || !existingTitle.equalsIgnoreCase(incomingTitle))) {
                validateProjectTitleDuplicationForUpdate(incomingTitle, command.id());
            }
        }

        // ProjectVo.updateFrom() 사용 - 업데이트 시 자동으로 검증 수행
        ProjectVo updatedProjectVo = ProjectVo.updateFrom(
                existingProject,
                command.title(),
                command.description(),
                command.content(),
                command.category(),
                command.subCategory(),
                command.startDate(),
                command.endDate(),
                command.githubUrl(),
                command.externalUrl(),
                command.demoUrl(),
                command.thumbnailUrl(),
                command.maxParticipants()
        );

        // Command Repository를 통한 저장 (VO 전달)
        ProjectVo savedProjectVo = commandRepository.save(updatedProjectVo);

        log.info("Domain: Project updated successfully - ID: {}", savedProjectVo.id());

        return savedProjectVo;
    }

    /**
     * 프로젝트 삭제
     */
    public void deleteProject(DeleteProjectCommand command) {
        log.info("Domain: Deleting project from command - ID: {}", command.id());

        // 프로젝트 존재 여부 확인 (Repository에서 직접 ProjectVo 반환)
        ProjectVo existingProject = queryRepository.findByIdAndDeletedAtIsNull(command.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다: " + command.id()));

        // 권한 검증: STAFF 이상이거나 작성자 본인인지 확인
        validateProjectDeletePermission(command.requesterId(), existingProject.creatorId());

        // Command Repository를 통한 Soft Delete
        commandRepository.deleteById(existingProject.id());

        log.info("Domain: Project deleted successfully - ID: {}", existingProject.id());
    }

    /**
     * 프로젝트 첨부파일 업로드
     */
    public String uploadProjectAttachment(Long projectId, Long memberId, MultipartFile file) {
        log.info("Domain: Uploading project attachment for project ID: {}, member ID: {}", projectId, memberId);

        // S3에 첨부파일 업로드
        String attachmentUrl = commandRepository.uploadProjectAttachment(projectId, memberId, file);

        log.info("Domain: Project attachment uploaded successfully for project ID: {}, member ID: {}, URL: {}", projectId, memberId, attachmentUrl);
        return attachmentUrl;
    }

    // ================================================================
    // QUERY OPERATIONS
    // ================================================================

    /**
     * 프로젝트 단건 조회
     */
    public ProjectVo getProjectById(GetProjectByIdQuery query) {
        log.info("Domain: Getting project from query - ID: {}", query.id());

        // ✅ Repository에서 직접 ProjectVo 반환 (ReadModel 변환 불필요)
        ProjectVo projectVo = queryRepository.findProjectDetailById(query.id())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다: " + query.id()));

        log.info("Domain: Project found - ID: {}", projectVo.id());
        return projectVo;
    }

    /**
     * 전체 프로젝트 조회 (페이징)
     */
    public Page<ProjectSummaryVo> getAllProjects(GetAllProjectsQuery query) {
        log.info("Domain: Getting all projects from query");

        ProjectSearchCriteriaVo emptyCriteria = ProjectSearchCriteriaVo.empty();
        ProjectSearchResultVo result = queryRepository.findProjects(emptyCriteria, query.pageable());

        // ✅ Repository에서 직접 ProjectSummaryVo 반환 (변환 불필요)
        Page<ProjectSummaryVo> projectPage = new PageImpl<>(result.projects(), query.pageable(), result.totalElements());

        log.info("Domain: Found {} projects", projectPage.getTotalElements());
        return projectPage;
    }

    /**
     * 복합 검색 조건으로 프로젝트 검색 (고급 검색 지원)
     */
    public Page<ProjectSummaryVo> searchProjectsByCriteria(SearchProjectsQuery query) {
        log.info("Domain: Searching projects from query - keyword: {}, semester: {}, category: {}, status: {}",
                query.keyword(), query.semester(), query.category(), query.status());

        // projectStatus 필드명 검증
        if (query.status() != null && !query.status().trim().isEmpty()) {
            validateProjectStatusField(query.status());
        }

        // Query를 ProjectSearchCriteria로 변환 (고급 검색 필드 포함)
        ProjectSearchCriteriaVo criteria = ProjectSearchCriteriaVo.ofAdvanced(
                query.keyword(),
                query.semester(),
                query.category(),
                query.subCategory(),
                query.status(),
                query.techStack()
        );

        ProjectSearchResultVo result = queryRepository.findProjects(criteria, query.pageable());

        // ✅ Repository에서 직접 ProjectSummaryVo 반환 (변환 불필요)
        Page<ProjectSummaryVo> projectPage = new PageImpl<>(result.projects(), query.pageable(), result.totalElements());

        log.info("Domain: Found {} projects by advanced criteria", projectPage.getTotalElements());
        return projectPage;
    }

    /**
     * projectStatus 필드명 검증
     * projectStatus가 아니면 에러를 발생시킴
     */
    private void validateProjectStatusField(String status) {
        // 실제로는 status 값이 유효한 ProjectStatus 값인지 검증
        try {
            ProjectStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid project status value. Expected one of [READY, INPROGRESS, COMPLETED] but got: " + status);
        }
        log.debug("Domain: Project status field validation passed for: {}", status);
    }

    /**
     * 특정 멤버가 생성한 완료된 프로젝트 목록 조회
     * 완료 조건: endedAt이 현재 시간보다 이전이고, 삭제되지 않은 프로젝트
     */
    public Page<ProjectSummaryVo> getCompletedProjectsByMember(GetCompletedProjectsByMemberQuery query) {
        log.info("Domain: Getting completed projects by member - memberId: {}", query.memberId());

        // Repository에서 완료된 프로젝트 조회
        Page<ProjectSummaryVo> completedProjects = queryRepository.findCompletedProjectsByMember(
                query.memberId(),
                query.pageable()
        );

        log.info("Domain: Found {} completed projects for member: {}",
                completedProjects.getTotalElements(), query.memberId());

        return completedProjects;
    }

    /**
     * 특정 멤버가 생성한 완료된 프로젝트 목록 조회 (리스트 버전)
     * 페이징 없이 전체 조회
     */
    public List<ProjectSummaryVo> getCompletedProjectsListByMember(Long memberId) {
        log.info("Domain: Getting completed projects list by member - memberId: {}", memberId);
        List<ProjectSummaryVo> completedProjects = queryRepository.findCompletedProjectsListByMember(memberId);

        log.info("Domain: Found {} completed projects for member: {}", completedProjects.size(), memberId);

        return completedProjects;
    }


    // ================================================================
    // PRIVATE VALIDATION METHODS (Repository 의존성이 필요한 검증만)
    // ================================================================

    /**
     * 프로젝트 생성자 존재 확인
     */
    private void validateCreatorExists(Long creatorId) {
        try {
            memberDomainService.getMemberVo(new GetMemberByIdQuery(creatorId));
            log.debug("Domain: Project creator validation passed - {}", creatorId);
        } catch (DomainException e) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_CREATOR,
                    "프로젝트 생성자를 찾을 수 없습니다: " + creatorId);
        }
    }

    /**
     * 프로젝트 수정 권한 검증
     * STAFF 이상의 관리자이거나 프로젝트 생성자 본인만 수정 가능
     */
    private void validateProjectUpdatePermission(Long requesterId, Long projectCreatorId) {
        log.debug("Domain: Validating project update permission - requesterId: {}, creatorId: {}",
                requesterId, projectCreatorId);

        // 작성자 본인인 경우 수정 허용
        if (requesterId.equals(projectCreatorId)) {
            log.debug("Domain: Update permission granted - requester is project creator");
            return;
        }

        // STAFF 이상 관리자 권한 확인
        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Update permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_ACCESS_DENIED,
                    "프로젝트를 수정할 권한이 없습니다");
        }

        // 권한이 없는 경우
        log.warn("Domain: Update permission denied - requesterId: {}, creatorId: {}", requesterId, projectCreatorId);
        throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_ACCESS_DENIED,
                "프로젝트를 수정할 권한이 없습니다");
    }

    /**
     * 프로젝트 삭제 권한 검증
     * STAFF 이상의 관리자이거나 프로젝트 생성자 본인만 삭제 가능
     */
    private void validateProjectDeletePermission(Long requesterId, Long projectCreatorId) {
        log.debug("Domain: Validating project delete permission - requesterId: {}, creatorId: {}",
                requesterId, projectCreatorId);

        // 작성자 본인인 경우 삭제 허용
        if (requesterId.equals(projectCreatorId)) {
            log.debug("Domain: Delete permission granted - requester is project creator");
            return;
        }

        // STAFF 이상 관리자 권한 확인
        try {
            MemberVo requesterMember = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
            MemberRole requesterRole = requesterMember.role();

            if (MemberRole.isStaffOrAbove(requesterRole)) {
                log.debug("Domain: Delete permission granted - requester is staff or above: {}", requesterRole);
                return;
            }
        } catch (DomainException e) {
            log.warn("Domain: Requester not found: {}", requesterId);
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_ACCESS_DENIED,
                    "프로젝트를 삭제할 권한이 없습니다");
        }

        // 권한이 없는 경우
        log.warn("Domain: Delete permission denied - requesterId: {}, creatorId: {}", requesterId, projectCreatorId);
        throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_ACCESS_DENIED,
                "프로젝트를 삭제할 권한이 없습니다");
    }


    /**
     * 프로젝트 제목 중복 검증 (생성 시)
     */
    private void validateProjectTitleDuplication(String title) {
        if (queryRepository.existsByTitle(title)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_TITLE,
                    "이미 존재하는 프로젝트 제목입니다: " + title);
        }
        log.debug("Domain: Project title duplication validation passed - {}", title);
    }

    /**
     * 프로젝트 종료
     */
    public ProjectVo endProject(EndProjectCommand command) {
        log.info("Domain: Ending project from command - ID: {}", command.projectId());

        // 기존 프로젝트 조회
        ProjectVo existingProject = queryRepository.findById(command.projectId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다: " + command.projectId()));

        // 권한 검증: STAFF 이상이거나 프로젝트 생성자인지 확인
        validateProjectEndPermission(command.requesterId(), existingProject.creatorId());

        // 이미 종료된 프로젝트인지 검증
        if (existingProject.endDate() != null && existingProject.endDate().isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION,
                    "이미 종료된 프로젝트입니다");
        }

        // 제출 단계: 종료는 승인 시 처리. 여기서는 변경 없이 반환.
        log.info("Domain: Project end submission initiated - ID: {}", existingProject.id());
        return existingProject;
    }

    // ===== Commands previously in CommandService moved behind command repository =====
    public void updateResultSubmission(Long projectId, OffsetDateTime submittedAt,
                                       ResultSubmitStatus status,
                                       String attachmentUrl) {
        commandRepository.updateResultSubmission(projectId, submittedAt, status, attachmentUrl);
    }

    public void approveEnd(Long projectId, OffsetDateTime endedAt,
                           ResultSubmitStatus status) {
        commandRepository.approveEnd(projectId, endedAt, status);
    }

    public void rejectEnd(Long projectId, ResultSubmitStatus status,
                          OffsetDateTime now) {
        commandRepository.rejectEnd(projectId, status, now);
    }

    public void bulkSoftDeleteById(Long projectId, OffsetDateTime deletedAt) {
        commandRepository.bulkSoftDeleteById(projectId, deletedAt);
    }

    public Optional<String> getResultAttachmentUrlById(Long projectId) {
        return commandRepository.getResultAttachmentUrlById(projectId);
    }

    /**
     * 종료 제출 정보 조회 (계층: Domain -> QueryRepository)
     */
    public ProjectEndSubmissionInfoVo getEndSubmissionInfo(Long projectId) {
        return queryRepository.getEndSubmissionInfo(projectId)
                .orElse(new ProjectEndSubmissionInfoVo(
                        projectId,
                        null, // status (ProjectStatus)
                        null, // resultSubmitStatus
                        null, // submittedAt
                        null, // attachmentUrl
                        null, // category
                        null, // subCategory
                        null, // title
                        null, // description
                        null, // creatorId
                        null, // creatorName
                        null, // creatorGrade
                        null, // startedAt
                        null, // endedAt
                        null, // currentParticipantNumber
                        null  // maxParticipantNumber
                ));
    }

    public List<ProjectEndSubmissionInfoVo> getEndSubmissionsInProgress() {
        return queryRepository.findEndSubmissionsInProgress();
    }

    /**
     * 프로젝트 종료 권한 검증
     */
    private void validateProjectEndPermission(Long requesterId, Long creatorId) {
        // 요청자 정보 조회
        MemberVo requester = memberDomainService.getMemberVo(new GetMemberByIdQuery(requesterId));
        
        // STAFF 이상이거나 프로젝트 생성자인지 확인
        if (!MemberRole.isStaffOrAbove(requester.role()) && !requesterId.equals(creatorId)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION,
                    "프로젝트 종료 권한이 없습니다. 프로젝트 생성자이거나 STAFF 이상이어야 합니다.");
        }
        
        log.debug("Domain: Project end permission validation passed - requesterId: {}, creatorId: {}", 
                requesterId, creatorId);
    }

    /**
     * 프로젝트 제목 중복 검증 (수정 시 - 자신 제외)
     */
    private void validateProjectTitleDuplicationForUpdate(String title, Long projectId) {
        if (queryRepository.existsByTitleAndIdNot(title, projectId)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_TITLE,
                    "이미 존재하는 프로젝트 제목입니다: " + title);
        }
        log.debug("Domain: Project title duplication validation passed for update - {}", title);
    }
}