package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GracePeriodService {

    private final MemberDomainService memberDomainService;

    @Transactional
    public void applyExpiredGracePeriods() {
        log.info("Application: Applying expired grace periods for UPSOLVER members");

        memberDomainService.applyGracePeriodForGrantingPenalties();

        log.info("Application: Grace period penalties applied successfully");
    }
}

