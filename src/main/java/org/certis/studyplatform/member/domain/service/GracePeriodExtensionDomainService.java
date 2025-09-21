package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.GracePeriodVo;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.domain.vo.MemberSearchForAdminVo;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.shared.util.GracePeriodCalculator;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * 유예기간 연장 도메인 서비스
 * 
 * 스터디/프로젝트 승인 시 참가자들의 유예기간을 연장하는 비즈니스 로직을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GracePeriodExtensionDomainService {

    private final MemberCommandRepository memberCommandRepository;
    private final StudyParticipantQueryRepository studyParticipantQueryRepository;
    private final ProjectParticipantQueryRepository projectParticipantQueryRepository;
    private final MemberQueryRepository memberQueryRepository;

    /**
     * 스터디 승인 시 모든 참가자의 유예기간 연장
     *
     * @param studyId 승인된 스터디 ID
     * @param studyStartDate 스터디 시작일
     * @param studyEndDate 스터디 종료일
     */
    public void extendGracePeriodForApprovedStudy(Long studyId, OffsetDateTime studyStartDate, OffsetDateTime studyEndDate) {
        log.info("Domain: Extending grace period for approved study - studyId: {}", studyId);

        // 1. 스터디 참가자들 조회 (페이징 없이 모든 참가자)
        List<StudyParticipantSummaryVo> participants = getAllStudyParticipants(studyId);
        
        log.info("Domain: Found {} participants for study - studyId: {}", participants.size(), studyId);
        for (StudyParticipantSummaryVo participant : participants) {
            log.info("Domain: Participant - memberId: {}, status: {}", participant.memberId(), participant.status());
        }
        
        if (participants.isEmpty()) {
            log.warn("Domain: No participants found for study - studyId: {}", studyId);
            return;
        }

        // 2. 새로운 유예기간 계산
        OffsetDateTime newGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            studyStartDate, studyEndDate
        );

        if (newGracePeriod == null) {
            log.warn("Domain: Could not calculate grace period for study - studyId: {}", studyId);
            return;
        }

        // 3. UPSOLVER 참가자에게만 유예기간 업데이트
        for (StudyParticipantSummaryVo participant : participants) {
            boolean isUpsolverMember = isUpsolver(participant.memberId());
            log.info("Domain: Member {} is Upsolver: {}", participant.memberId(), isUpsolverMember);
            if (isUpsolverMember) {
                log.info("Domain: About to update grace period for memberId={}, newGracePeriod={}", 
                    participant.memberId(), newGracePeriod);
                updateMemberGracePeriod(participant.memberId(), newGracePeriod);
                log.info("Domain: Completed grace period update for memberId={}", participant.memberId());
            } else {
                log.debug("Domain: Skipping grace period update for non-Upsolver memberId={}", participant.memberId());
            }
        }

        log.info("Domain: Grace period extended for {} participants in study - studyId: {}, newGracePeriod: {}", 
            participants.size(), studyId, newGracePeriod);
    }

    /**
     * 프로젝트 승인 시 모든 참가자의 유예기간 연장
     *
     * @param projectId 승인된 프로젝트 ID
     * @param projectStartDate 프로젝트 시작일
     * @param projectEndDate 프로젝트 종료일
     */
    public void extendGracePeriodForApprovedProject(Long projectId, OffsetDateTime projectStartDate, OffsetDateTime projectEndDate) {
        log.info("Domain: Extending grace period for approved project - projectId: {}", projectId);

        // 1. 프로젝트 참가자들 조회 (페이징 없이 모든 참가자)
        List<ProjectParticipantSummaryVo> participants = getAllProjectParticipants(projectId);
        
        if (participants.isEmpty()) {
            log.warn("Domain: No participants found for project - projectId: {}", projectId);
            return;
        }

        // 2. 새로운 유예기간 계산
        OffsetDateTime newGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            projectStartDate, projectEndDate
        );

        if (newGracePeriod == null) {
            log.warn("Domain: Could not calculate grace period for project - projectId: {}", projectId);
            return;
        }

        // 3. UPSOLVER 참가자에게만 유예기간 업데이트
        for (ProjectParticipantSummaryVo participant : participants) {
            if (isUpsolver(participant.memberId())) {
                updateMemberGracePeriod(participant.memberId(), newGracePeriod);
            } else {
                log.debug("Domain: Skipping grace period update for non-Upsolver memberId={}", participant.memberId());
            }
        }

        log.info("Domain: Grace period extended for {} participants in project - projectId: {}, newGracePeriod: {}", 
            participants.size(), projectId, newGracePeriod);
    }

    /**
     * 스터디의 모든 참가자 조회 (승인된 참가자만)
     */
    private List<StudyParticipantSummaryVo> getAllStudyParticipants(Long studyId) {
        log.info("Domain: Fetching all approved participants for study - studyId: {}", studyId);
        List<StudyParticipantSummaryVo> participants = studyParticipantQueryRepository.findAllApprovedByStudyId(studyId);
        log.info("Domain: Found {} approved participants for study - studyId: {}", participants.size(), studyId);
        return participants;
    }

    /**
     * 프로젝트의 모든 참가자 조회 (승인된 참가자만)
     */
    private List<ProjectParticipantSummaryVo> getAllProjectParticipants(Long projectId) {
        return projectParticipantQueryRepository.findAllApprovedByProjectId(projectId);
    }

    /**
     * 회원의 유예기간 업데이트
     */
    private void updateMemberGracePeriod(Long memberId, OffsetDateTime newGracePeriod) {
        try {
            MemberIdVo memberIdVo = MemberIdVo.of(memberId);
            GracePeriodVo gracePeriodVo = GracePeriodVo.of(newGracePeriod);
            
            memberCommandRepository.updateGracePeriod(memberIdVo, gracePeriodVo);
            
            log.info("Domain: Grace period updated for member - memberId: {}, newGracePeriod: {}", 
                memberId, newGracePeriod);
        } catch (Exception e) {
            log.error("Domain: Failed to update grace period for member - memberId: {}, error: {}", 
                memberId, e.getMessage(), e);
            // 개별 회원 업데이트 실패가 전체 프로세스를 중단시키지 않도록 처리
        }
    }

    /**
     * 멤버가 UPSOLVER 인지 확인
     */
    private boolean isUpsolver(Long memberId) {
        try {
            log.info("Domain: Checking if member {} is Upsolver", memberId);
            Optional<MemberVo> memberOpt = memberQueryRepository.findById(MemberIdVo.of(memberId));
            if (memberOpt.isEmpty()) {
                log.warn("Domain: Member {} not found in database", memberId);
                return false;
            }
            MemberVo member = memberOpt.get();
            boolean isUpsolver = member.role() == MemberRole.UPSOLVER;
            log.info("Domain: Member {} role: {}, isUpsolver: {}", memberId, member.role(), isUpsolver);
            return isUpsolver;
        } catch (Exception e) {
            log.warn("Domain: Failed to determine role for memberId={}, skipping. error={}", memberId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 유예기간 연장 조건 확인
     * 
     * @param currentGracePeriod 현재 유예기간
     * @param newGracePeriod 새로 계산된 유예기간
     * @return 연장 가능 여부
     */
    public boolean shouldExtendGracePeriod(OffsetDateTime currentGracePeriod, OffsetDateTime newGracePeriod) {
        if (newGracePeriod == null) {
            return false;
        }

        // 현재 유예기간이 없거나 새로운 유예기간이 더 긴 경우에만 연장
        if (currentGracePeriod == null) {
            return true;
        }

        return newGracePeriod.isAfter(currentGracePeriod);
    }

    /**
     * D-Day 신청 여부 확인
     * 
     * @param gracePeriodEnd 유예기간 종료일
     * @return D-Day 여부
     */
    public boolean isDDayApplication(OffsetDateTime gracePeriodEnd) {
        if (gracePeriodEnd == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        return now.toLocalDate().equals(gracePeriodEnd.toLocalDate());
    }

    /**
     * 활동 기간 계산 (주 단위)
     */
    public long calculateActivityDurationWeeks(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.WEEKS.between(startDate, endDate);
    }

    /**
     * 스터디 조기 종료 시 유예기간 재조정
     * 
     * @param studyId 조기 종료된 스터디 ID
     * @param studyStartDate 스터디 시작일
     * @param actualEndDate 실제 종료일 (조기 종료일)
     */
    public void adjustGracePeriodForEarlyTerminatedStudy(Long studyId, OffsetDateTime studyStartDate, OffsetDateTime actualEndDate) {
        log.info("Domain: Adjusting grace period for early terminated study - studyId: {}, actualEndDate: {}", studyId, actualEndDate);

        // 1. 스터디 참가자들 조회 (승인된 참가자만)
        List<StudyParticipantSummaryVo> participants = getAllStudyParticipants(studyId);
        
        if (participants.isEmpty()) {
            log.warn("Domain: No participants found for early terminated study - studyId: {}", studyId);
            return;
        }

        // 2. 조기 종료된 활동의 실제 기간으로 유예기간 재계산
        OffsetDateTime adjustedGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            studyStartDate, actualEndDate
        );

        if (adjustedGracePeriod == null) {
            log.warn("Domain: Could not calculate adjusted grace period for early terminated study - studyId: {}", studyId);
            return;
        }

        // 3. UPSOLVER 참가자들의 유예기간을 재조정된 값으로 업데이트
        for (StudyParticipantSummaryVo participant : participants) {
            if (isUpsolver(participant.memberId())) {
                // 현재 유예기간 조회
                OffsetDateTime currentGracePeriod = memberQueryRepository.findGracePeriodByMemberId(participant.memberId()).orElse(null);
                
                // 조기 종료로 인해 유예기간이 단축되는 경우에만 업데이트
                if (shouldAdjustGracePeriodForEarlyTermination(currentGracePeriod, adjustedGracePeriod)) {
                    updateMemberGracePeriod(participant.memberId(), adjustedGracePeriod);
                    log.info("Domain: Grace period adjusted for early terminated study - memberId: {}, oldGracePeriod: {}, newGracePeriod: {}", 
                        participant.memberId(), currentGracePeriod, adjustedGracePeriod);
                } else {
                    log.debug("Domain: Grace period not adjusted for member - memberId: {}, currentGracePeriod: {}, adjustedGracePeriod: {}", 
                        participant.memberId(), currentGracePeriod, adjustedGracePeriod);
                }
            }
        }

        log.info("Domain: Grace period adjustment completed for early terminated study - studyId: {}, adjustedGracePeriod: {}", 
            studyId, adjustedGracePeriod);
    }

    /**
     * 프로젝트 조기 종료 시 유예기간 재조정
     * 
     * @param projectId 조기 종료된 프로젝트 ID
     * @param projectStartDate 프로젝트 시작일
     * @param actualEndDate 실제 종료일 (조기 종료일)
     */
    public void adjustGracePeriodForEarlyTerminatedProject(Long projectId, OffsetDateTime projectStartDate, OffsetDateTime actualEndDate) {
        log.info("Domain: Adjusting grace period for early terminated project - projectId: {}, actualEndDate: {}", projectId, actualEndDate);

        // 1. 프로젝트 참가자들 조회 (승인된 참가자만)
        List<ProjectParticipantSummaryVo> participants = getAllProjectParticipants(projectId);
        
        if (participants.isEmpty()) {
            log.warn("Domain: No participants found for early terminated project - projectId: {}", projectId);
            return;
        }

        // 2. 조기 종료된 활동의 실제 기간으로 유예기간 재계산
        OffsetDateTime adjustedGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            projectStartDate, actualEndDate
        );

        if (adjustedGracePeriod == null) {
            log.warn("Domain: Could not calculate adjusted grace period for early terminated project - projectId: {}", projectId);
            return;
        }

        // 3. UPSOLVER 참가자들의 유예기간을 재조정된 값으로 업데이트
        for (ProjectParticipantSummaryVo participant : participants) {
            if (isUpsolver(participant.memberId())) {
                // 현재 유예기간 조회
                OffsetDateTime currentGracePeriod = memberQueryRepository.findGracePeriodByMemberId(participant.memberId()).orElse(null);
                
                // 조기 종료로 인해 유예기간이 단축되는 경우에만 업데이트
                if (shouldAdjustGracePeriodForEarlyTermination(currentGracePeriod, adjustedGracePeriod)) {
                    updateMemberGracePeriod(participant.memberId(), adjustedGracePeriod);
                    log.info("Domain: Grace period adjusted for early terminated project - memberId: {}, oldGracePeriod: {}, newGracePeriod: {}", 
                        participant.memberId(), currentGracePeriod, adjustedGracePeriod);
                } else {
                    log.debug("Domain: Grace period not adjusted for member - memberId: {}, currentGracePeriod: {}, adjustedGracePeriod: {}", 
                        participant.memberId(), currentGracePeriod, adjustedGracePeriod);
                }
            }
        }

        log.info("Domain: Grace period adjustment completed for early terminated project - projectId: {}, adjustedGracePeriod: {}", 
            projectId, adjustedGracePeriod);
    }

    /**
     * 조기 종료 시 유예기간 재조정 여부 확인
     * 
     * @param currentGracePeriod 현재 유예기간
     * @param adjustedGracePeriod 조정된 유예기간
     * @return 재조정 필요 여부
     */
    private boolean shouldAdjustGracePeriodForEarlyTermination(OffsetDateTime currentGracePeriod, OffsetDateTime adjustedGracePeriod) {
        if (adjustedGracePeriod == null) {
            return false;
        }

        // 현재 유예기간이 없으면 조정된 유예기간으로 설정
        if (currentGracePeriod == null) {
            return true;
        }

        // 조기 종료로 인해 유예기간이 단축되는 경우에만 재조정
        // (더 긴 유예기간으로는 조정하지 않음 - 이는 다른 활동으로 인한 연장일 수 있음)
        return adjustedGracePeriod.isBefore(currentGracePeriod);
    }
}
