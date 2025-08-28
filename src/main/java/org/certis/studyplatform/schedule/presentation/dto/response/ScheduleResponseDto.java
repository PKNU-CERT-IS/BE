package org.certis.studyplatform.schedule.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponseDto {
    private Long scheduleId;
    private String title;
    private String description;
    private String type;
    private String place;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String status;
    private OffsetDateTime createdAt;
}