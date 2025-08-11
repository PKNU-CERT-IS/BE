package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberFindOneRequestDto {

    @NotBlank(message = "이름은 필수입니다")
    private Long memberId;
}
