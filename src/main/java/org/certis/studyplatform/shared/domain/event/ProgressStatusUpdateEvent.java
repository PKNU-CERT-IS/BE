package org.certis.studyplatform.shared.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Progress Status Update Event
 * 
 * 시간 기반 상태 업데이트를 위한 이벤트
 */
@Getter
@RequiredArgsConstructor
public class ProgressStatusUpdateEvent {
    
    private final String entityType; // "STUDY" or "PROJECT"
    private final Long entityId;
    private final OffsetDateTime currentTime;
    
    public static ProgressStatusUpdateEvent forStudy(Long studyId, OffsetDateTime currentTime) {
        return new ProgressStatusUpdateEvent("STUDY", studyId, currentTime);
    }
    
    public static ProgressStatusUpdateEvent forProject(Long projectId, OffsetDateTime currentTime) {
        return new ProgressStatusUpdateEvent("PROJECT", projectId, currentTime);
    }
}
