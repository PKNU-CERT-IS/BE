package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// 유예기간 부여 요청 Dto

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GrantGracePeriodRequestDto {

    @NotNull(message = "회원 ID는 필수입니다")
    @Positive(message = "회원 ID는 양수여야 합니다")
    private Long memberId;

    @NotNull(message = "유예기간은 필수입니다")
    @Future(message = "유예기간은 현재 시간보다 미래여야 합니다")
    private OffsetDateTime gracePeriod;
}
