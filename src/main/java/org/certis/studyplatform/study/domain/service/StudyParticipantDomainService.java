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

    // ================================================================
    // COMMAND OPERATIONS
    // ================================================================

    /**
     * 스터디 참가 신청
     */
    public StudyParticipantCreatedVo createParticipant(CreateStudyParticipantCommand command) {
        log.info("Domain: Creating participant request - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());

        // 1. 스터디 존재 및 상태 검증
        StudyVo study = validateStudyForJoin(command.studyId());

        // 2. 스터디 생성자가 자신의 스터디에 참가 신청하는 것 방지
        if (study.creatorId().equals(command.memberId())) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_STATUS,
                    "스터디 생성자는 자신의 스터디에 참가 신청할 수 없습니다.");
        }

        // 3. 중복 신청 검증
        validateDuplicateParticipation(command.studyId(), command.memberId());

        // 4. 참가자 수 제한 검증
        validateParticipantLimit(command.studyId(), study.maxParticipants());

        // 5. 새로운 참가 신청 생성
        StudyParticipantVo participantVo = StudyParticipantVo.createNew(
                command.studyId(), command.memberId());

        // 6. 저장
        StudyParticipantCreatedVo result = commandRepository.save(participantVo);

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

        // 3. 취소 처리 (소프트 삭제)
        commandRepository.deleteByStudyIdAndMemberId(command.studyId(), command.memberId());

        log.info("Domain: Participant request cancelled - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());
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
        StudyParticipantStatusUpdatedVo result = commandRepository.updateStatus(updatedParticipant);

        log.info("Domain: Participant approved - ID: {}", result.id());
        return result;
    }

    /**
     * 스터디 참가 거절
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

        // 4. 거절 처리
        StudyParticipantVo updatedParticipant = participant.updateStatus(StudyParticipantStatus.REJECTED);
        StudyParticipantStatusUpdatedVo result = commandRepository.updateStatus(updatedParticipant);

        log.info("Domain: Participant rejected - ID: {}", result.id());
        return result;
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
        if (queryRepository.existsByStudyIdAndMemberId(studyId, memberId)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "이미 참가 신청한 스터디입니다.");
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

        if (!study.creatorId().equals(requesterId)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_PERMISSION_DENINED,
                    "스터디 생성자만 참가 승인/거절을 할 수 있습니다.");
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