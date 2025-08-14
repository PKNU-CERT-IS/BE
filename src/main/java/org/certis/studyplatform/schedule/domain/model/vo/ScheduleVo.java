package org.certis.studyplatform.schedule.domain.model.vo;

import java.time.OffsetDateTime;

public record ScheduleVo(
        Long id,
        Long memberId,  // memberId 추가
        String title,
        String description,
        String type,
        String place,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String status,
        OffsetDateTime createdAt
) {
    public static ScheduleVo of(Long id, Long memberId, String title, String description, String type, String place,
                                OffsetDateTime startedAt, OffsetDateTime endedAt, String status, OffsetDateTime createdAt) {
        return new ScheduleVo(id, memberId, title, description, type, place, startedAt, endedAt, status, createdAt);
    }
}
