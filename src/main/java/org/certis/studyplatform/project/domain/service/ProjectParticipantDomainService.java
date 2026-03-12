package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.project.application.object.command.*;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// import removed: OffsetDateTime no longer used after switching to status-based validation

/**
 * Project Participant Domain Service
 *
 * Clean Architecture Domain Layer
 * 프로젝트 참가 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectParticipantDomainService {

    private final ProjectParticipantCommandRepository commandRepository;
    private final ProjectParticipantQueryRepository queryRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final MemberQueryRepository memberQueryRepository;

    @Value("${benchmark.mode:after}")
    private String benchmarkMode;

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 프로젝트 참가 신청
     */
    public ProjectParticipantCreatedVo createParticipant(CreateProjectParticipantCommand command) {
        log.info("Domain: Creating participant request - projectId: {}, memberId: {}",
                command.projectId(), command.memberId());

        // 1. 중복 신청 검증 (중복일 때 우선적으로 에러 발생시키도록 순서 조정)
        validateDuplicateParticipation(command.projectId(), command.memberId());

        // 2. 프로젝트 존재 및 상태 검증
        ProjectVo project = validateProjectForJoin(command.projectId());

        // 3. 프로젝트 생성자가 자신의 프로젝트에 참가 신청하는 것 방지
        if (project.creatorId().equals(command.memberId())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_STATUS,
                    "프로젝트 생성자는 자신의 프로젝트에 참가 신청할 수 없습니다.");
        }

        // 4. 신청 제한 규칙 검증 (프로젝트: 진행 중 1개 초과 금지)
        enforceApplicationLimits(command.memberId());

        // 5. 참가자 수 제한 검증
        validateParticipantLimit(command.projectId(), project.maxParticipants());

        // 6. 새로운 참가 신청 생성 및 저장
        ProjectParticipantVo participantVo = ProjectParticipantVo.createNew(
                command.projectId(), command.memberId());
        
        ProjectParticipantCreatedVo result = commandRepository.save(participantVo);

        log.info("Domain: Participant request created - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 신청 취소
     */
    public void cancelParticipant(CancelProjectParticipantCommand command) {
        log.info("Domain: Cancelling participant request - projectId: {}, memberId: {}",
                command.projectId(), command.memberId());

        // 1. 참가 신청 조회 (상태 무관)
        ProjectParticipantVo participant = queryRepository
                .findPendingByProjectIdAndMemberId(command.projectId(), command.memberId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. 본인만 취소 가능
        if (!participant.memberId().equals(command.memberId())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED,
                    "본인의 참가 신청만 취소할 수 있습니다.");
        }

        // 3. 하드 삭제 수행
        commandRepository.deleteByIdHard(participant.id());

        log.info("Domain: Approved participant cancelled (hard deleted) - participantId: {}",
                participant.id());
    }

    /**
     * 프로젝트 참가 승인
     */
    public ProjectParticipantStatusUpdatedVo approveParticipant(UpdateProjectParticipantStatusCommand command) {
        log.info("Domain: Approving participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        // 1. 참가 신청 조회
        ProjectParticipantVo participant = queryRepository.findById(command.participantId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. PENDING 상태인지 확인
        if (!participant.isPending()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                    "대기 중인 참가 신청만 승인할 수 있습니다.");
        }

        // 3. 프로젝트 생성자 권한 확인
        validateProjectLeaderPermission(participant.projectId(), command.requesterId());

        // 4. 참가자 수 제한 재검증 (동시성 고려)
        ProjectVo project = (isLegacyBenchmarkMode()
                ? projectQueryRepository.findById(participant.projectId())
                : projectQueryRepository.findByIdForUpdate(participant.projectId()))
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다."));
        validateParticipantLimit(participant.projectId(), project.maxParticipants());

        // 5. 승인 처리
        ProjectParticipantVo updatedParticipant = participant.updateStatus(ProjectParticipantStatus.APPROVED);
        ProjectParticipantStatusUpdatedVo result = commandRepository.updateStatus(updatedParticipant);

        log.info("Domain: Participant approved - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 거절 (소프트 삭제)
     */
    public ProjectParticipantStatusUpdatedVo rejectParticipant(UpdateProjectParticipantStatusCommand command) {
        log.info("Domain: Rejecting participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        // 1. 참가 신청 조회
        ProjectParticipantVo participant = queryRepository.findById(command.participantId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. PENDING 상태인지 확인
        if (!participant.isPending()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED,
                    "대기 중인 참가 신청만 거절할 수 있습니다.");
        }

        // 3. 프로젝트 생성자 권한 확인
        validateProjectLeaderPermission(participant.projectId(), command.requesterId());

        // 4. 거절 처리: 소프트 삭제 수행 (상태는 논리적으로 REJECTED로 간주)
        commandRepository.softDeleteById(participant.id());

        ProjectParticipantStatusUpdatedVo result = ProjectParticipantStatusUpdatedVo.of(
                participant.id(),
                participant.projectId(),
                participant.memberId(),
                participant.status(),
                ProjectParticipantStatus.REJECTED
        );

        log.info("Domain: Participant rejected (soft deleted) - ID: {}", result.id());
        return result;
    }

    /**
     * 승인된 참가자 취소 (하드 삭제)
     */
    public void cancelApprovedParticipant(Long participantId, Long requesterId) {
        log.info("Domain: Cancelling approved participant - participantId: {}, requesterId: {}",
                participantId, requesterId);

        // 1. 참가 신청 조회
        ProjectParticipantVo participant = queryRepository.findById(participantId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. APPROVED 상태인지 확인
        if (!participant.isApproved()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                    "승인된 참가자만 취소할 수 있습니다.");
        }

        // 3. 프로젝트 생성자 권한 확인
        validateProjectLeaderPermission(participant.projectId(), requesterId);

        // 4. 하드 삭제 처리
        commandRepository.deleteByIdHard(participantId);

        log.info("Domain: Approved participant cancelled (hard deleted) - ID: {}", participantId);
    }

    /**
     * 소프트 삭제된 참가 신청 복원 (가장 최신 1건)
     * @return 복원 성공 여부
     */
    public boolean restoreLatestSoftDeleted(Long projectId, Long memberId) {
        int restored = commandRepository.restoreByProjectIdAndMemberId(projectId, memberId);
        return restored > 0;
    }

    private boolean isLegacyBenchmarkMode() {
        return "before".equalsIgnoreCase(benchmarkMode);
    }

    /**
     * 프로젝트 생성자를 자동으로 참가자로 등록 (APPROVED 상태)
     * 프로젝트 생성 시에만 호출되는 메소드
     */
    public ProjectParticipantCreatedVo registerProjectCreatorAsParticipant(Long projectId, Long creatorId) {
        log.info("Domain: Registering project creator as participant - projectId: {}, creatorId: {}",
                projectId, creatorId);

        // 1. 이미 등록된 참가자인지 확인 (중복 방지)
        if (queryRepository.existsByProjectIdAndMemberId(projectId, creatorId)) {
            log.warn("Domain: Creator already registered as participant - projectId: {}, creatorId: {}",
                    projectId, creatorId);
            // 이미 존재하는 경우 기존 정보를 반환
            ProjectParticipantVo existingParticipant = queryRepository
                    .findByProjectIdAndMemberId(projectId, creatorId)
                    .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                            "참가자 정보를 찾을 수 없습니다."));

            return ProjectParticipantCreatedVo.of(
                    existingParticipant.id(),
                    existingParticipant.projectId(),
                    existingParticipant.memberId(),
                    existingParticipant.status(),
                    existingParticipant.createdAt()
                );
        }

        // 2. 프로젝트 생성자를 APPROVED 상태로 참가자 등록
        ProjectParticipantVo creatorParticipant = ProjectParticipantVo.createApproved(
                projectId, creatorId);

        // 3. 저장
        ProjectParticipantCreatedVo result = commandRepository.save(creatorParticipant);

        log.info("Domain: Project creator registered as participant - ID: {}", result.id());
        return result;
    }




    // ================================================================
    // PRIVATE VALIDATION METHODS
    // ================================================================

    /**
     * 참가 가능한 프로젝트인지 검증
     */
    private ProjectVo validateProjectForJoin(Long projectId) {
        ProjectVo project = projectQueryRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다."));

        // 프로젝트 종료 여부 확인 (시간 비교가 아닌, DB에 저장된 status 기준)
        if (ProjectStatus.fromStatusString(project.status()).isCompleted()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_DEADLINE_PASSED,
                    "종료된 프로젝트에는 참가할 수 없습니다.");
        }

        return project;
    }

    /**
     * 중복 참가 신청 검증
     */
    private void validateDuplicateParticipation(Long projectId, Long memberId) {
        // Check actual status: only PENDING or APPROVED should block re-application
        queryRepository.findByProjectIdAndMemberId(projectId, memberId)
                .ifPresent(existing -> {
                    if (existing.isPending() || existing.isApproved()) {
                        throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                                "이미 참가 신청한 프로젝트입니다.");
                    }
                });
    }

    /**
     * 참가자 수 제한 검증
     */
    private void validateParticipantLimit(Long projectId, Integer maxParticipants) {
        if (maxParticipants == null) {
            return; // 제한 없음
        }

        long currentCount = queryRepository.countApprovedParticipantsByProjectId(projectId);
        if (currentCount >= maxParticipants) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                    "프로젝트 정원이 가득 찼습니다.");
        }
    }

    /**
     * 프로젝트 생성자 권한 확인
     */
    private void validateProjectLeaderPermission(Long projectId, Long requesterId) {
        ProjectVo project = projectQueryRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND,
                        "프로젝트를 찾을 수 없습니다."));

        boolean isLeader = project.creatorId().equals(requesterId);
        boolean isAdmin = memberQueryRepository.findRoleByMemberId(new MemberIdVo(requesterId))
                .map(MemberRole::isStaffOrAbove)
                .orElse(false);

        if (!(isLeader || isAdmin)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED,
                    "프로젝트 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }
    }

    private void enforceApplicationLimits(Long memberId) {
        long activeProjectsJoined = queryRepository.countActiveProjectsByMemberId(memberId);

        // Use lightweight aggregate count to avoid nested reads causing rollback-only
        long activeProjectsCreated = projectQueryRepository.countActiveProjectsCreatedByMemberId(memberId);

        long activeProjects = activeProjectsJoined + activeProjectsCreated;
        if (activeProjects >= 1) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION,
                    "진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }
    }

    /**
     * 프로젝트 생성자를 참가자에서 제외 (프로젝트 삭제 시 사용)
     * 프로젝트 삭제 시에만 호출되는 메소드
     */
    public void removeProjectCreatorFromParticipants(Long projectId, Long creatorId) {
        log.info("Domain: Removing project creator from participants - projectId: {}, creatorId: {}",
                projectId, creatorId);

        // 1. 생성자의 참가자 정보 조회
        queryRepository.findByProjectIdAndMemberId(projectId, creatorId)
                .ifPresent(participant -> {
                    // 2. 소프트 삭제 수행
                    commandRepository.deleteByProjectIdAndMemberId(projectId, creatorId);
                    log.info("Domain: Project creator removed from participants - projectId: {}, creatorId: {}",
                            projectId, creatorId);
                });
    }
}
