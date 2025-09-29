package org.certis.studyplatform.shared.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Progress Status Scheduler
 * 
 * 정기적으로 Study와 Project의 상태를 업데이트하는 스케줄러
 * 매 시간마다 실행되어 시간 기반 상태 변경을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressStatusScheduler {

    private final ApplicationEventPublisher eventPublisher;
    private final StudyQueryRepository studyQueryRepository;
    private final ProjectQueryRepository projectQueryRepository;

    /**
     * 매 시간마다 실행되는 스케줄러
     * 현재 시간이 started_at 이후인 Study와 Project의 상태를 업데이트
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행 (3600000ms)
    public void updateProgressStatus() {
        log.info("Starting progress status update scheduler");
        
        try {
            OffsetDateTime currentTime = OffsetDateTime.now();
            
            // Study 상태 업데이트
            updateStudyStatuses(currentTime);
            
            // Project 상태 업데이트
            updateProjectStatuses(currentTime);
            
            log.info("Completed progress status update scheduler");
            
        } catch (Exception e) {
            log.error("Error in progress status update scheduler", e);
        }
    }

    private void updateStudyStatuses(OffsetDateTime currentTime) {
        try {
            // APPROVED 상태이면서 started_at이 현재 시간 이전인 Study들을 찾아서 이벤트 발행
            List<Long> studyIds = studyQueryRepository.findApprovedStudiesStartedBefore(currentTime);
            
            for (Long studyId : studyIds) {
                eventPublisher.publishEvent(ProgressStatusUpdateEvent.forStudy(studyId, currentTime));
            }
            
            log.info("Published {} study progress status update events", studyIds.size());
            
        } catch (Exception e) {
            log.error("Error updating study statuses", e);
        }
    }

    private void updateProjectStatuses(OffsetDateTime currentTime) {
        try {
            // APPROVED 상태이면서 started_at이 현재 시간 이전인 Project들을 찾아서 이벤트 발행
            List<Long> projectIds = projectQueryRepository.findApprovedProjectsStartedBefore(currentTime);
            
            for (Long projectId : projectIds) {
                eventPublisher.publishEvent(ProgressStatusUpdateEvent.forProject(projectId, currentTime));
            }
            
            log.info("Published {} project progress status update events", projectIds.size());
            
        } catch (Exception e) {
            log.error("Error updating project statuses", e);
        }
    }
}
