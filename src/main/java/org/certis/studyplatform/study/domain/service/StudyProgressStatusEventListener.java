package org.certis.studyplatform.study.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.shared.domain.service.ProgressStatusService;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.certis.studyplatform.study.domain.vo.StudyVo;

/**
 * Study Progress Status Event Listener
 * 
 * 시간 기반 상태 업데이트 이벤트를 처리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyProgressStatusEventListener {

    private final StudyQueryRepository studyQueryRepository;
    private final StudyCommandRepository studyCommandRepository;
    private final ProgressStatusService progressStatusService;

    @EventListener
    @Transactional
    public void handleProgressStatusUpdate(ProgressStatusUpdateEvent event) {
        if (!"STUDY".equals(event.getEntityType())) {
            return;
        }

        try {
            log.info("Processing study progress status update for study ID: {}", event.getEntityId());
            
            var studyVoOpt = studyQueryRepository.findVoByIdForStatusCheck(event.getEntityId());
            if (studyVoOpt.isEmpty()) {
                log.warn("Study not found with ID: {}", event.getEntityId());
                return;
            }

            var vo = studyVoOpt.get();
            var currentTime = event.getCurrentTime();
            
            // 현재 시간이 started_at 이후인지 확인
            if (currentTime.isAfter(vo.startDate())) {
                var result = progressStatusService.calculateStudyStatus(
                    org.certis.studyplatform.study.domain.StudyStatus.fromStatusString(vo.status()),
                    vo.resultSubmitStatus(),
                    vo.startDate(),
                    vo.endDate(),
                    currentTime
                );

                // 상태가 변경된 경우에만 업데이트
                if (!org.certis.studyplatform.study.domain.StudyStatus.fromStatusString(vo.status()).equals(result.status()) || 
                    !vo.resultSubmitStatus().equals(result.resultSubmitStatus())) {
                    // 변경된 상태를 반영한 VO 저장
                    StudyVo updated = new StudyVo(
                        vo.id(), vo.title(), vo.description(), vo.content(), vo.category(), vo.subCategory(),
                        vo.startDate(), vo.endDate(), vo.createdAt(), vo.updatedAt(), vo.creatorId(), vo.creatorName(),
                        vo.creatorGrade(), vo.creatorProfileImageUrl(), vo.semester(),
                        ((org.certis.studyplatform.study.domain.StudyStatus) result.status()).name(),
                        result.resultSubmitStatus(),
                        vo.maxParticipants(), vo.currentParticipants(), vo.isParticipantable(),
                        vo.attached(), vo.summaryVoList(), vo.participantVoList()
                    );
                    studyCommandRepository.save(updated);
                    
                    log.info("Updated study {} status to {} and resultSubmitStatus to {}", 
                        event.getEntityId(), result.status(), result.resultSubmitStatus());
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing study progress status update for study ID: {}", 
                event.getEntityId(), e);
        }
    }
}
