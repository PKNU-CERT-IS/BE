package org.certis.studyplatform.member.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.GracePeriodService;
// import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GracePeriodScheduler {

    private final GracePeriodService gracePeriodService;

    // 자정 00:10 에 실행
    // @Scheduled(cron = "0 10 0 * * *")
    public void processGracePeriods() {
        log.info("Scheduler: Grace period job started at 00:10");

        try {
            gracePeriodService.applyExpiredGracePeriods();
            log.info("Scheduler: Grace period job completed successfully");
        } catch (Exception e) {
            log.error("Scheduler: Grace period job failed", e);
        }
    }
}