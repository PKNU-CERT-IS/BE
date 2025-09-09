package org.certis.studyplatform.schedule.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// 무조건 STATUS 는 APPROVE
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminScheduleCreateRequestDto {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 20, message = "제목은 20자 이하여야 합니다")
    private String title;

    @Size(max = 50, message = "설명은 50자 이하여야 합니다")
    private String description;

    @NotBlank(message = "스케줄 타입은 필수입니다")
    private String type;

    @NotBlank(message = "장소는 필수입니다")
    private String place;

    @NotNull(message = "시작 시간은 필수입니다")
    private OffsetDateTime startedAt;

    @NotNull(message = "종료 시간은 필수입니다")
    private OffsetDateTime endedAt;

}
