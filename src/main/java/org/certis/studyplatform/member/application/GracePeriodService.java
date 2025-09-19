package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.service.GracePeriodExtensionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 유예기간 관련 응용 서비스
 * 
 * 스케줄러와 도메인 서비스 사이의 중간 계층
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GracePeriodService {

    private final MemberDomainService memberDomainService;
    private final GracePeriodExtensionDomainService gracePeriodExtensionDomainService;

    /**
     * 만료된 유예기간 처리
     * 
     * 스케줄러에서 호출되어 유예기간이 만료된 회원들에게 벌점 부여
     */
    public void applyExpiredGracePeriods() {
        log.info("Application: Starting expired grace period processing");
        
        try {
            memberDomainService.applyGracePeriodForGrantingPenalties();
            log.info("Application: Expired grace period processing completed successfully");
        } catch (Exception e) {
            log.error("Application: Failed to process expired grace periods", e);
            throw e; // 스케줄러에서 에러를 감지할 수 있도록 re-throw
        }
    }

    /**
     * 스터디 승인 시 유예기간 연장
     * 
     * @param studyId 승인된 스터디 ID
     * @param studyStartDate 스터디 시작일
     * @param studyEndDate 스터디 종료일
     */
    public void extendGracePeriodForApprovedStudy(Long studyId, OffsetDateTime studyStartDate, OffsetDateTime studyEndDate) {
        log.info("Application: Extending grace period for approved study - studyId: {}", studyId);
        
        try {

            gracePeriodExtensionDomainService.extendGracePeriodForApprovedStudy(studyId, studyStartDate, studyEndDate);
            log.info("Application: Grace period extension completed for study - studyId: {}", studyId);
        } catch (Exception e) {
            log.error("Application: Failed to extend grace period for study - studyId: {}, error: {}", studyId, e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 프로젝트 승인 시 유예기간 연장
     * 
     * @param projectId 승인된 프로젝트 ID
     * @param projectStartDate 프로젝트 시작일
     * @param projectEndDate 프로젝트 종료일
     */
    public void extendGracePeriodForApprovedProject(Long projectId, OffsetDateTime projectStartDate, OffsetDateTime projectEndDate) {
        log.info("Application: Extending grace period for approved project - projectId: {}", projectId);
        
        try {
            gracePeriodExtensionDomainService.extendGracePeriodForApprovedProject(projectId, projectStartDate, projectEndDate);
            log.info("Application: Grace period extension completed for project - projectId: {}", projectId);
        } catch (Exception e) {
            log.error("Application: Failed to extend grace period for project - projectId: {}, error: {}", projectId, e.getMessage(), e);
            throw e;
        }
    }
}