package org.certis.studyplatform.schedule.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminScheduleDeleteRequestDto {

    @NotNull(message = "스케줄 ID는 필수입니다")
    @Positive(message = "스케줄 ID는 양수여야 합니다")
    private Long scheduleId;

}
