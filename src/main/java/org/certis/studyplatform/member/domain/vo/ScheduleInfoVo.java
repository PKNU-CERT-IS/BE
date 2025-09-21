package org.certis.studyplatform.member.domain.vo;

import java.time.OffsetDateTime;

/**
 * 스케줄 정보 Value Object
 * 
 * 프로필에서 사용하는 오늘의 스케줄 정보
 */
public record ScheduleInfoVo(
        Long id,
        String title,
        String place,
        String type,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
    
    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ScheduleInfoVo {
        // null 체크 및 기본값 설정
        title = title != null ? title : "";
        place = place != null ? place : "";
        type = type != null ? type : "";
    }
    
    /**
     * 정적 팩토리 메서드
     */
    public static ScheduleInfoVo of(Long id, String title, String place, String type, 
                                   OffsetDateTime startTime, OffsetDateTime endTime) {
        return new ScheduleInfoVo(id, title, place, type, startTime, endTime);
    }
    
    /**
     * 스케줄이 오늘인지 확인
     */
    public boolean isToday() {
        if (startTime == null) return false;
        
        OffsetDateTime now = OffsetDateTime.now();
        return startTime.toLocalDate().equals(now.toLocalDate());
    }
    
    /**
     * 스케줄이 진행 중인지 확인
     */
    public boolean isInProgress() {
        if (startTime == null || endTime == null) return false;
        
        OffsetDateTime now = OffsetDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
    
    /**
     * 스케줄이 완료되었는지 확인
     */
    public boolean isCompleted() {
        if (endTime == null) return false;
        
        OffsetDateTime now = OffsetDateTime.now();
        return now.isAfter(endTime);
    }
}
