package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.shared.domain.service.ProgressStatusService;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Progress Status Event Listener
 * 
 * 시간 기반 상태 업데이트 이벤트를 처리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectProgressStatusEventListener {

    private final ProjectQueryRepository projectQueryRepository;
    private final ProjectCommandRepository projectCommandRepository;
    private final ProgressStatusService progressStatusService;

    @EventListener
    @Transactional
    public void handleProgressStatusUpdate(ProgressStatusUpdateEvent event) {
        if (!"PROJECT".equals(event.getEntityType())) {
            return;
        }

        try {
            log.info("Processing project progress status update for project ID: {}", event.getEntityId());

            var projectVoOpt = projectQueryRepository.findVoByIdForStatusCheck(event.getEntityId());
            if (projectVoOpt.isEmpty()) {
                log.warn("Project not found with ID: {}", event.getEntityId());
                return;
            }

            var vo = projectVoOpt.get();
            var currentTime = event.getCurrentTime();
            
            // 현재 시간이 started_at 이후인지 확인
            if (currentTime.isAfter(vo.startDate())) {
                var result = progressStatusService.calculateProjectStatus(
                    ProjectStatus.valueOf(vo.status()),
                    vo.resultSubmitStatus(),
                    vo.startDate(),
                    vo.endDate(),
                    currentTime
                );

                // 상태가 변경된 경우에만 업데이트
                if (!ProjectStatus.valueOf(vo.status()).equals(result.status()) || 
                    !vo.resultSubmitStatus().equals(result.resultSubmitStatus())) {

                    ProjectVo updated = ProjectVo.of(
                        vo.id(), vo.title(), vo.description(), vo.content(), vo.category(), vo.subCategory(),
                        vo.startDate(), vo.endDate(), vo.creatorId(), vo.creatorName(), vo.creatorGrade(),
                        vo.semester(), ((ProjectStatus) result.status()).name(), result.resultSubmitStatus(),
                        vo.githubUrl(), vo.externalUrl(), vo.demoUrl(), vo.thumbnailUrl(),
                        vo.maxParticipants(), vo.currentParticipants(), vo.isParticipantable(),
                        vo.attached(), vo.meetingSummaryVos()
                    );
                    projectCommandRepository.save(updated);
                    
                    log.info("Updated project {} status to {} and resultSubmitStatus to {}", 
                        event.getEntityId(), result.status(), result.resultSubmitStatus());
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing project progress status update for project ID: {}", 
                event.getEntityId(), e);
        }
    }
}
