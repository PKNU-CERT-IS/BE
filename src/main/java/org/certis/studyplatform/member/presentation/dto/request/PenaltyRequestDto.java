package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyRequestDto {

    @NotNull(message = "회원 ID는 필수입니다")
    @Positive(message = "회원 ID는 양수여야 합니다")
    private Long memberId;

    @NotNull(message = "벌점은 필수입니다")
    @Positive(message = "벌점은 양수여야 합니다")
    private Integer penaltyPoints;
}
