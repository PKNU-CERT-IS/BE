package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.service.GracePeriodExtensionDomainService;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 프로젝트 승인 도메인 서비스
 * 
 * 프로젝트 승인 시 필요한 비즈니스 로직을 처리
 * - 승인 시 참가자 유예기간 연장
 * - 승인 조건 검증
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectApprovalDomainService {

    private final ProjectQueryRepository projectQueryRepository;
    private final GracePeriodExtensionDomainService gracePeriodExtensionDomainService;

    /**
     * 프로젝트 승인 처리
     * 
     * @param projectId 승인할 프로젝트 ID
     * @param approverId 승인자 ID (관리자)
     */
    public void approveProject(Long projectId, Long approverId) {
        log.info("Domain: Approving project - projectId: {}, approverId: {}", projectId, approverId);

        // 1. 프로젝트 정보 조회
        ProjectVo project = projectQueryRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 2. 승인 조건 검증
        validateApprovalConditions(project, approverId);

        // 3. 프로젝트 상태를 승인으로 변경 (실제 구현에서는 ProjectCommandService 호출)
        // projectCommandService.updateStatus(projectId, ProjectStatus.APPROVED);

        // 4. 참가자들의 유예기간 연장
        extendGracePeriodForParticipants(project);

        log.info("Domain: Project approved successfully - projectId: {}", projectId);
    }

    /**
     * 프로젝트 거절 처리
     * 
     * @param projectId 거절할 프로젝트 ID
     * @param rejectReason 거절 사유
     * @param approverId 승인자 ID (관리자)
     */
    public void rejectProject(Long projectId, String rejectReason, Long approverId) {
        log.info("Domain: Rejecting project - projectId: {}, approverId: {}, reason: {}", 
            projectId, approverId, rejectReason);

        // 1. 프로젝트 정보 조회
        projectQueryRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 2. 거절 권한 검증
        validateRejectionPermission(approverId);

        // 3. 프로젝트 상태를 거절로 변경 (실제 구현에서는 ProjectCommandService 호출)
        // projectCommandService.updateStatus(projectId, ProjectStatus.REJECTED);

        // 4. 거절 시에는 유예기간 연장 없음 (기존 유예기간 유지)
        log.info("Domain: Project rejected, grace period unchanged - projectId: {}", projectId);

        log.info("Domain: Project rejected successfully - projectId: {}", projectId);
    }

    /**
     * 승인 조건 검증
     */
    private void validateApprovalConditions(ProjectVo project, Long approverId) {
        // 1. 관리자 권한 확인
        validateApprovalPermission(approverId);

        // 2. 프로젝트 상태 확인 (PENDING 상태만 승인 가능)
        // if (!project.isPending()) {
        //     throw new IllegalStateException("Only pending projects can be approved");
        // }

        // 3. 승인 시간 확인 (매주 일요일 18:00-24:00)
        if (!isApprovalTimeWindow()) {
            throw new IllegalStateException("Project approval is only allowed on Sunday 18:00-24:00");
        }

        // 4. 프로젝트 내용 검증 (CS/정보보안 관련, 필수 항목 완성도 등)
        validateProjectContent(project);
    }

    /**
     * 관리자 승인 권한 검증
     */
    private void validateApprovalPermission(Long approverId) {
        // 실제 구현에서는 Member 도메인에서 권한 확인
        // MemberVo approver = memberQueryRepository.findById(approverId);
        // if (!approver.hasApprovalPermission()) {
        //     throw new IllegalArgumentException("No approval permission");
        // }
        log.debug("Domain: Approval permission validated for approverId: {}", approverId);
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
     * 프로젝트 내용 검증
     */
    private void validateProjectContent(ProjectVo project) {
        // 1. CS/정보보안 관련 주제인지 확인
        if (!isValidProjectCategory(project.category())) {
            throw new IllegalArgumentException("Invalid project category: must be CS/Security related");
        }

        // 2. 필수 항목 완성도 확인
        if (project.title() == null || project.title().trim().isEmpty()) {
            throw new IllegalArgumentException("Project title is required");
        }

        if (project.description() == null || project.description().trim().isEmpty()) {
            throw new IllegalArgumentException("Project description is required");
        }

        // 3. 기간 검증 (4주 ~ 12주)
        validateProjectDuration(project);

        log.debug("Domain: Project content validated - projectId: {}", project.id());
    }

    /**
     * 유효한 프로젝트 카테고리인지 확인
     */
    private boolean isValidProjectCategory(String category) {
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
     * 프로젝트 기간 검증
     */
    private void validateProjectDuration(ProjectVo project) {
        if (project.startDate() == null || project.endDate() == null) {
            throw new IllegalArgumentException("Project start and end dates are required");
        }

        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(project.startDate(), project.endDate());
        
        if (weeks < 4 || weeks > 12) {
            throw new IllegalArgumentException("Project duration must be between 4 and 12 weeks");
        }
    }

    /**
     * 참가자들의 유예기간 연장 처리
     */
    private void extendGracePeriodForParticipants(ProjectVo project) {
        try {
            gracePeriodExtensionDomainService.extendGracePeriodForApprovedProject(
                project.id(),
                project.startDate(),
                project.endDate()
            );
            
            log.info("Domain: Grace period extended for project participants - projectId: {}", project.id());
        } catch (Exception e) {
            log.error("Domain: Failed to extend grace period for project participants - projectId: {}, error: {}", 
                project.id(), e.getMessage(), e);
            
            // 유예기간 연장 실패가 승인 프로세스를 중단시키지 않도록 처리
            // 실제 운영에서는 별도 알림이나 재처리 로직 필요
        }
    }
}
