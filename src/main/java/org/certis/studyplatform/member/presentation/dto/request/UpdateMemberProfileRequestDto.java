package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileRequestDto {
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    private String profileImageUrl;
}
