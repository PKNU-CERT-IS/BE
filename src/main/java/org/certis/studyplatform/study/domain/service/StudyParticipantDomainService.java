package org.certis.studyplatform.study.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.study.application.object.command.CancelStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.CreateStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyParticipantStatusCommand;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.repository.StudyParticipantCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.springframework.stereotype.Service;

import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;

import java.time.OffsetDateTime;

/**
 * Study Participant Domain Service
 *
 * Clean Architecture Domain Layer
 * 스터디 참가 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyParticipantDomainService {

    private final StudyParticipantCommandRepository commandRepository;
    private final StudyParticipantQueryRepository queryRepository;
    private final StudyQueryRepository studyQueryRepository;
    private final ProjectParticipantQueryRepository projectParticipantQueryRepository;
    private final org.certis.studyplatform.project.domain.repository.ProjectQueryRepository projectQueryRepository;
    private final MemberQueryRepository memberQueryRepository;

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 스터디 참가 신청
     */
    public StudyParticipantCreatedVo createParticipant(CreateStudyParticipantCommand command) {
        log.info("Domain: Creating participant request - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());

        // 1. 중복 신청 검증 (테스트 기대: 중복일 때 우선적으로 에러 발생)
        validateDuplicateParticipation(command.studyId(), command.memberId());

        // 2. 스터디 존재 및 상태 검증
        StudyVo study = validateStudyForJoin(command.studyId());

        // 3. 신청 제한 규칙 검증 (도메인 상한 규칙을 우선 적용)
        enforceApplicationLimits(command.memberId());

        // 4. 스터디 생성자가 자신의 스터디에 참가 신청하는 것 방지
        if (study.creatorId().equals(command.memberId())) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_STATUS,
                    "스터디 생성자는 자신의 스터디에 참가 신청할 수 없습니다.");
        }

        // 5. 참가자 수 제한 검증 (동시성 고려를 위해 마지막에 재확인)
        validateParticipantLimit(command.studyId(), study.maxParticipants());

        // 6. 기존 소프트 삭제(거절/취소) 레코드 복원 시도
        int restored = commandRepository.restoreByStudyIdAndMemberId(command.studyId(), command.memberId());

        StudyParticipantCreatedVo result;
        if (restored > 0) {
            // 복원된 레코드 조회 후 상태를 PENDING으로 전환 (가시성 이슈 대비하여 재시도 경로 포함)
            java.util.Optional<StudyParticipantVo> restoredOpt = queryRepository
                    .findByStudyIdAndMemberId(command.studyId(), command.memberId());

            if (restoredOpt.isEmpty()) {
                // 드물게 같은 트랜잭션에서 즉시 조회가 비어 보일 수 있으므로, 저장 시도 → 유니크 충돌 시 재조회
                try {
                    StudyParticipantVo participantVo = StudyParticipantVo.createNew(
                            command.studyId(), command.memberId());
                    result = commandRepository.save(participantVo);
                    // 정상적으로 저장되면 그대로 반환
                    log.warn("Domain: Restored participant not immediately visible; created new instead - studyId: {}, memberId: {}",
                            command.studyId(), command.memberId());
                    return result;
                } catch (DomainException ex) {
                    // 유니크 충돌로 기존 레코드가 존재함을 의미 → 재조회하여 반환
                    restoredOpt = queryRepository.findByStudyIdAndMemberId(command.studyId(), command.memberId());
                    StudyParticipantVo fetched = restoredOpt.orElseThrow(() -> new DomainException(
                            ExceptionStatus.STUDY_DOMAIN_NOT_FOUND, "복원된 참가 신청을 찾을 수 없습니다."));
                    if (!fetched.isPending()) {
                        StudyParticipantVo pendingVo = fetched.updateStatus(StudyParticipantStatus.PENDING);
                        commandRepository.updateStatus(pendingVo, command.memberId());
                    }
                    result = StudyParticipantCreatedVo.of(
                            fetched.id(),
                            fetched.studyId(),
                            fetched.memberId(),
                            StudyParticipantStatus.PENDING,
                            fetched.createdAt()
                    );
                    return result;
                }
            }

            StudyParticipantVo restoredVo = restoredOpt.get();
            if (!restoredVo.isPending()) {
                StudyParticipantVo pendingVo = restoredVo.updateStatus(StudyParticipantStatus.PENDING);
                commandRepository.updateStatus(pendingVo, command.memberId());
            }

            // 복원된 동일 레코드를 반환 형태로 맞추기 위해 CreatedVo를 구성
            result = StudyParticipantCreatedVo.of(
                    restoredVo.id(),
                    restoredVo.studyId(),
                    restoredVo.memberId(),
                    StudyParticipantStatus.PENDING,
                    restoredVo.createdAt()
            );
        } else {
            // 7. 새로운 참가 신청 생성 및 저장
            StudyParticipantVo participantVo = StudyParticipantVo.createNew(
                    command.studyId(), command.memberId());
            result = commandRepository.save(participantVo);
        }

        log.info("Domain: Participant request created - ID: {}", result.id());
        return result;
    }

    /**
     * 스터디 참가 신청 취소
     */
    public void cancelParticipant(CancelStudyParticipantCommand command) {
        log.info("Domain: Cancelling participant request - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());

        // 1. PENDING 상태의 참가 신청 조회
        StudyParticipantVo participant = queryRepository
                .findPendingByStudyIdAndMemberId(command.studyId(), command.memberId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "대기 중인 참가 신청을 찾을 수 없습니다."));

        // 2. 본인만 취소 가능
        if (!participant.memberId().equals(command.memberId())) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "본인의 참가 신청만 취소할 수 있습니다.");
        }

        // 3. 취소 처리 (하드 삭제)
        commandRepository.deleteByIdHard(participant.id());

        log.info("Domain: Pending participant cancelled (hard deleted) - participantId: {}",
                participant.id());
    }

    /**
     * 스터디 참가 승인
     */
    public StudyParticipantStatusUpdatedVo approveParticipant(UpdateStudyParticipantStatusCommand command) {
        log.info("Domain: Approving participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        // 1. 참가 신청 조회
        StudyParticipantVo participant = queryRepository.findById(command.participantId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. PENDING 상태인지 확인
        if (!participant.isPending()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "대기 중인 참가 신청만 승인할 수 있습니다.");
        }

        // 3. 스터디 생성자 권한 확인
        validateStudyLeaderPermission(participant.studyId(), command.requesterId());

        // 4. 참가자 수 제한 재검증 (동시성 고려)
        StudyVo study = studyQueryRepository.findById(participant.studyId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다."));
        validateParticipantLimit(participant.studyId(), study.maxParticipants());

        // 5. 승인 처리
        StudyParticipantVo updatedParticipant = participant.updateStatus(StudyParticipantStatus.APPROVED);
        StudyParticipantStatusUpdatedVo result = commandRepository.updateStatus(updatedParticipant, command.requesterId());

        log.info("Domain: Participant approved - ID: {}", result.id());
        return result;
    }

    /**
     * 스터디 참가 거절 (소프트 삭제)
     */
    public StudyParticipantStatusUpdatedVo rejectParticipant(UpdateStudyParticipantStatusCommand command) {
        log.info("Domain: Rejecting participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        // 1. 참가 신청 조회
        StudyParticipantVo participant = queryRepository.findById(command.participantId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. PENDING 상태인지 확인
        if (!participant.isPending()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "대기 중인 참가 신청만 거절할 수 있습니다.");
        }

        // 3. 스터디 생성자 권한 확인
        validateStudyLeaderPermission(participant.studyId(), command.requesterId());

        // 4. 거절 처리 (소프트 삭제)
        commandRepository.softDeleteById(command.participantId());

        // 5. 결과 VO 생성 (거절된 상태로 반환)
        StudyParticipantStatusUpdatedVo result = StudyParticipantStatusUpdatedVo.of(
                participant.id(),
                participant.studyId(),
                participant.memberId(),
                participant.status(), // 이전 상태 (PENDING)
                StudyParticipantStatus.REJECTED, // 현재 상태 (REJECTED)
                command.requesterId());

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
        StudyParticipantVo participant = queryRepository.findById(participantId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "참가 신청을 찾을 수 없습니다."));

        // 2. APPROVED 상태인지 확인
        if (!participant.isApproved()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "승인된 참가자만 취소할 수 있습니다.");
        }

        // 3. 스터디 생성자 권한 확인
        validateStudyLeaderPermission(participant.studyId(), requesterId);

        // 4. 하드 삭제 처리
        commandRepository.deleteByIdHard(participantId);

        log.info("Domain: Approved participant cancelled (hard deleted) - ID: {}", participantId);
    }

    /**
     * 스터디 생성자를 자동으로 참가자로 등록 (APPROVED 상태)
     * 스터디 생성 시에만 호출되는 메소드
     */
    public StudyParticipantCreatedVo registerStudyCreatorAsParticipant(Long studyId, Long creatorId) {
        log.info("Domain: Registering study creator as participant - studyId: {}, creatorId: {}",
                studyId, creatorId);

        // 1. 이미 등록된 참가자인지 확인 (중복 방지)
        if (queryRepository.existsByStudyIdAndMemberId(studyId, creatorId)) {
            log.warn("Domain: Creator already registered as participant - studyId: {}, creatorId: {}",
                    studyId, creatorId);
            // 이미 존재하는 경우 기존 정보를 반환
            StudyParticipantVo existingParticipant = queryRepository
                    .findByStudyIdAndMemberId(studyId, creatorId)
                    .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                            "참가자 정보를 찾을 수 없습니다."));

            return StudyParticipantCreatedVo.of(
                    existingParticipant.id(),
                    existingParticipant.studyId(),
                    existingParticipant.memberId(),
                    existingParticipant.status(),
                    existingParticipant.createdAt()
                );
        }

        // 2. 스터디 생성자를 APPROVED 상태로 참가자 등록
        StudyParticipantVo creatorParticipant = StudyParticipantVo.createApproved(
                studyId, creatorId);

        // 3. 저장
        StudyParticipantCreatedVo result = commandRepository.save(creatorParticipant);

        log.info("Domain: Study creator registered as participant - ID: {}", result.id());
        return result;
    }




    // ================================================================
    // PRIVATE VALIDATION METHODS
    // ================================================================

    /**
     * 참가 가능한 스터디인지 검증
     */
    private StudyVo validateStudyForJoin(Long studyId) {
        StudyVo study = studyQueryRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다."));

        // 스터디 종료 여부 확인
        OffsetDateTime now = OffsetDateTime.now();
        if (study.endDate() != null && study.endDate().isBefore(now)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_DEADLINE_PASSED);
        }

        return study;
    }

    /**
     * 중복 참가 신청 검증
     */
    private void validateDuplicateParticipation(Long studyId, Long memberId) {
        // Prefer checking actual status to be robust across repository implementations/caching
        queryRepository.findByStudyIdAndMemberId(studyId, memberId)
                .ifPresent(existing -> {
                    if (existing.isPending() || existing.isApproved()) {
                        throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                                "이미 참가 신청한 스터디입니다.");
                    }
                    // REJECTED/CANCELLED should be allowed to re-apply
                });
    }

    /**
     * 신청 제한 규칙 적용
     * - 진행 중인 study >= 2 면 추가 신청 불가
     * - 진행 중인 project >= 1 이고 진행 중인 study >= 1 이면 study 추가 신청 불가
     */
    private void enforceApplicationLimits(Long memberId) {
        long activeStudiesJoined = queryRepository.countActiveStudiesByMemberId(memberId);
        long activeProjectsJoined = projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId);

        // Include studies created by the member that are currently active
        long activeStudiesCreated = 0L;
        try {
            var activeStudiesResult = studyQueryRepository.findActiveStudies(org.springframework.data.domain.Pageable.unpaged());
            if (activeStudiesResult != null && activeStudiesResult.studies() != null) {
                activeStudiesCreated = activeStudiesResult.studies().stream()
                        .filter(s -> s != null && s.id() != null)
                        .map(s -> studyQueryRepository.findById(s.id()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .filter(full -> full.creatorId() != null && full.creatorId().equals(memberId))
                        .count();
            }
        } catch (Exception ignored) { }

        // Include projects created by the member that are currently active
        long activeProjectsCreated = 0L;
        try {
            var activeProjectsResult = projectQueryRepository.findActiveProjects(org.springframework.data.domain.Pageable.unpaged());
            if (activeProjectsResult != null && activeProjectsResult.projects() != null) {
                activeProjectsCreated = activeProjectsResult.projects().stream()
                        .filter(p -> p != null && p.id() != null)
                        .map(p -> projectQueryRepository.findById(p.id()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .filter(full -> full.creatorId() != null && full.creatorId().equals(memberId))
                        .count();
            }
        } catch (Exception ignored) { }

        long activeStudies = activeStudiesJoined + activeStudiesCreated;
        long activeProjects = activeProjectsJoined + activeProjectsCreated;

        // 프로젝트 미진행 시: 스터디 2개까지 허용 (즉, 3번째부터 제한)
        if (activeProjects == 0 && activeStudies >= 2) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "프로젝트 미진행 시 스터디 2개까지 가능합니다.");
        }

        // 프로젝트 진행 중이면 스터디는 최대 1개만 (즉, 2번째부터 제한)
        if (activeProjects >= 1 && activeStudies >= 1) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "프로젝트 진행 중에는 스터디 1개까지만 신청할 수 있습니다.");
        }
    }

    /**
     * 참가자 수 제한 검증
     */
    private void validateParticipantLimit(Long studyId, Integer maxParticipants) {
        if (maxParticipants == null) {
            return; // 제한 없음
        }

        long currentCount = queryRepository.countApprovedParticipantsByStudyId(studyId);
        if (currentCount >= maxParticipants) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "스터디 참가 인원이 가득찼습니다.");
        }
    }

    /**
     * 스터디 생성자 권한 확인
     */
    private void validateStudyLeaderPermission(Long studyId, Long requesterId) {
        StudyVo study = studyQueryRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.STUDY_DOMAIN_NOT_FOUND,
                        "스터디를 찾을 수 없습니다."));

        boolean isLeader = study.creatorId().equals(requesterId);
        boolean isAdmin = memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(requesterId))
                .map(org.certis.studyplatform.member.domain.MemberRole::isStaffOrAbove)
                .orElse(false);

        if (!(isLeader || isAdmin)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }
    }

    /**
     * 스터디 생성자를 참가자에서 제외 (스터디 삭제 시 사용)
     * 스터디 삭제 시에만 호출되는 메소드
     */
    public void removeStudyCreatorFromParticipants(Long studyId, Long creatorId) {
        log.info("Domain: Removing study creator from participants - studyId: {}, creatorId: {}",
                studyId, creatorId);

        // 1. 생성자의 참가자 정보 조회
        queryRepository.findByStudyIdAndMemberId(studyId, creatorId)
                .ifPresent(participant -> {
                    // 2. 소프트 삭제 수행
                    commandRepository.deleteByStudyIdAndMemberId(studyId, creatorId);
                    log.info("Domain: Study creator removed from participants - studyId: {}, creatorId: {}",
                            studyId, creatorId);
                });
    }
}