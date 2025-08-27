package org.certis.studyplatform.schedule.domain.model.vo;

import java.time.OffsetDateTime;

public record AdminScheduleVo(
        Long id,
        String title,
        String description,
        String type,
        String place,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String status,
        OffsetDateTime createdAt,
        Long memberId,
        String memberName
) {
    public static AdminScheduleVo of(Long id, String title, String description, String type, String place,
                                     OffsetDateTime startedAt, OffsetDateTime endedAt, String status,
                                     OffsetDateTime createdAt, Long memberId, String memberName) {
        return new AdminScheduleVo(id, title, description, type, place, startedAt, endedAt,
                status, createdAt, memberId, memberName);
    }
}