package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}