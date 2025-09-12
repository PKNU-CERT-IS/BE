package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.GracePeriodVo;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.MemberVo;
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
            if (isUpsolver(participant.memberId())) {
                updateMemberGracePeriod(participant.memberId(), newGracePeriod);
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
        // 실제 구현에서는 Repository에 findAllApprovedByStudyId 메서드가 필요
        // 현재는 기존 메서드를 활용하여 구현
        Page<StudyParticipantSummaryVo> participantPage = studyParticipantQueryRepository.findByStudyId(
            studyId, 
            org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED,
            Pageable.unpaged()
        );
        return participantPage.getContent();
    }

    /**
     * 프로젝트의 모든 참가자 조회 (승인된 참가자만)
     */
    private List<ProjectParticipantSummaryVo> getAllProjectParticipants(Long projectId) {
        // 실제 구현에서는 Repository에 findAllApprovedByProjectId 메서드가 필요
        // 현재는 기존 메서드를 활용하여 구현
        Page<ProjectParticipantSummaryVo> participantPage = projectParticipantQueryRepository.findByProjectId(
            projectId, 
            org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED,
            Pageable.unpaged()
        );
        return participantPage.getContent();
    }

    /**
     * 회원의 유예기간 업데이트
     */
    private void updateMemberGracePeriod(Long memberId, OffsetDateTime newGracePeriod) {
        try {
            MemberIdVo memberIdVo = MemberIdVo.of(memberId);
            GracePeriodVo gracePeriodVo = GracePeriodVo.of(newGracePeriod);
            
            memberCommandRepository.updateGracePeriod(memberIdVo, gracePeriodVo);
            
            log.debug("Domain: Grace period updated for member - memberId: {}, newGracePeriod: {}", 
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
            return memberQueryRepository.findById(MemberIdVo.of(memberId))
                    .map(MemberVo::role)
                    .map(role -> role == MemberRole.UPSOLVER)
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Domain: Failed to determine role for memberId={}, skipping. error={}", memberId, e.getMessage());
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
}
