package org.certis.studyplatform.schedule.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminScheduleUpdateRequestDto {

    @NotNull(message = "스케줄 ID는 필수입니다")
    @Positive(message = "스케줄 ID는 양수여야 합니다")

    private Long scheduleId;
    @NotBlank(message = "처리 상태는 필수입니다")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "상태는 APPROVED 또는 REJECTED만 가능합니다")
    private String status;

}
