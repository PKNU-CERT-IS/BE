package org.certis.studyplatform.study.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.service.GracePeriodExtensionDomainService;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 스터디 승인 도메인 서비스
 * 
 * 스터디 승인 시 필요한 비즈니스 로직을 처리
 * - 승인 시 참가자 유예기간 연장
 * - 승인 조건 검증
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyApprovalDomainService {

    private final StudyQueryRepository studyQueryRepository;
    private final GracePeriodExtensionDomainService gracePeriodExtensionDomainService;
    private final MemberDomainService memberDomainService;

    /**
     * 스터디 승인 처리
     * 
     * @param studyId 승인할 스터디 ID
     * @param approverId 승인자 ID (관리자)
     */
    public void approveStudy(Long studyId, Long approverId) {
        log.info("Domain: Approving study - studyId: {}, approverId: {}", studyId, approverId);

        // 1. 스터디 정보 조회
        StudyVo study = studyQueryRepository.findById(studyId)
            .orElseThrow(() -> new IllegalArgumentException("Study not found: " + studyId));

        // 2. 승인 조건 검증
        validateApprovalConditions(study, approverId);

        // 3. 스터디 상태를 승인으로 변경 (실제 구현에서는 StudyDomainService 혹은 Infrastructure layer method 호출)

        // 4. 참가자들의 유예기간 연장
        extendGracePeriodForParticipants(study);

        log.info("Domain: Study approved successfully - studyId: {}", studyId);
    }

    /**
     * 스터디 거절 처리
     * 
     * @param studyId 거절할 스터디 ID
     * @param rejectReason 거절 사유
     * @param approverId 승인자 ID (관리자)
     */
    public void rejectStudy(Long studyId, String rejectReason, Long approverId) {
        log.info("Domain: Rejecting study - studyId: {}, approverId: {}, reason: {}", 
            studyId, approverId, rejectReason);

        // 1. 스터디 정보 조회
        studyQueryRepository.findById(studyId)
            .orElseThrow(() -> new IllegalArgumentException("Study not found: " + studyId));

        // 2. 거절 권한 검증
        validateRejectionPermission(approverId);

        // 3. 스터디 상태를 거절로 변경 (실제 구현에서는 StudyCommandService 호출)
        // studyCommandService.updateStatus(studyId, StudyStatus.REJECTED);

        // 4. 거절 시에는 유예기간 연장 없음 (기존 유예기간 유지)
        log.info("Domain: Study rejected, grace period unchanged - studyId: {}", studyId);

        log.info("Domain: Study rejected successfully - studyId: {}", studyId);
    }

    /**
     * 승인 조건 검증
     */
    private void validateApprovalConditions(StudyVo study, Long approverId) {
        // 1. 관리자 권한 확인
        validateApprovalPermission(approverId);

        // 2. 스터디 상태 확인 (PENDING 상태만 승인 가능)
        // if (!study.isPending()) {
        //     throw new IllegalStateException("Only pending studies can be approved");
        // }

        // 3. 승인 시간 확인 (매주 일요일 18:00-24:00)
        if (!isApprovalTimeWindow()) {
            throw new IllegalStateException("Study approval is only allowed on Sunday 18:00-24:00");
        }

        // 4. 스터디 내용 검증 (CS/정보보안 관련, 필수 항목 완성도 등)
        validateStudyContent(study);
    }

    /**
     * 관리자 승인 권한 검증
     */
    private void validateApprovalPermission(Long approverId) {
        try {
            var approver = memberDomainService.getMemberVo(new GetMemberByIdQuery(approverId));
            MemberRole role = approver.role();
            if (!MemberRole.isStaffOrAbove(role)) {
                throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED, "승인 권한이 없습니다");
            }
            log.debug("Domain: Approval permission granted - approverId: {}, role: {}", approverId, role);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_ACCESS_DENIED, "승인 권한 검증 중 오류가 발생했습니다");
        }
    }

    /**
     * 거절 권한 검증
     */
    private void validateRejectionPermission(Long approverId) {
        validateApprovalPermission(approverId); // 승인/거절 권한은 동일
    }

    /**
     * 승인 시간 윈도우 확인 (매주 일요일 18:00-24:00)
     */
    private boolean isApprovalTimeWindow() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // 일요일인지 확인
        if (now.getDayOfWeek().getValue() != 7) { // 7 = Sunday
            return false;
        }

        // 18:00-24:00 시간대 확인
        int hour = now.getHour();
        return hour >= 18 && hour < 24;
    }

    /**
     * 스터디 내용 검증
     */
    private void validateStudyContent(StudyVo study) {
        // 1. CS/정보보안 관련 주제인지 확인
        if (!isValidStudyCategory(study.category())) {
            throw new IllegalArgumentException("Invalid study category: must be CS/Security related");
        }

        // 2. 필수 항목 완성도 확인
        if (study.title() == null || study.title().trim().isEmpty()) {
            throw new IllegalArgumentException("Study title is required");
        }

        if (study.description() == null || study.description().trim().isEmpty()) {
            throw new IllegalArgumentException("Study description is required");
        }

        // 3. 기간 검증 (1주 ~ 8주)
        validateStudyDuration(study);

        log.debug("Domain: Study content validated - studyId: {}", study.id());
    }

    /**
     * 유효한 스터디 카테고리인지 확인
     */
    private boolean isValidStudyCategory(String category) {
        if (category == null) {
            return false;
        }

        // CERT-IS 정책에 따른 유효한 카테고리
        String[] validCategories = {"CTF", "CS", "RED", "BLUE", "GRC", "MISC", "기타"};
        
        for (String validCategory : validCategories) {
            if (category.toUpperCase().startsWith(validCategory)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 스터디 기간 검증
     */
    private void validateStudyDuration(StudyVo study) {
        if (study.startDate() == null || study.endDate() == null) {
            throw new IllegalArgumentException("Study start and end dates are required");
        }

        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(study.startDate(), study.endDate());
        
        if (weeks < 1 || weeks > 8) {
            throw new IllegalArgumentException("Study duration must be between 1 and 8 weeks");
        }
    }

    /**
     * 참가자들의 유예기간 연장 처리
     */
    private void extendGracePeriodForParticipants(StudyVo study) {
        try {
            gracePeriodExtensionDomainService.extendGracePeriodForApprovedStudy(
                study.id(),
                study.startDate(),
                study.endDate()
            );
            
            log.info("Domain: Grace period extended for study participants - studyId: {}", study.id());
        } catch (Exception e) {
            log.error("Domain: Failed to extend grace period for study participants - studyId: {}, error: {}", 
                study.id(), e.getMessage(), e);
            
            // 유예기간 연장 실패가 승인 프로세스를 중단시키지 않도록 처리
            // 실제 운영에서는 별도 알림이나 재처리 로직 필요
        }
    }
}
