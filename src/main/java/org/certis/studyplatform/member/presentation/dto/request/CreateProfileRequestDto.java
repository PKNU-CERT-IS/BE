package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequestDto {

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;

    @NotBlank(message = "프로필 이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "프로필 이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Size(max = 500, message = "소개는 500자를 초과할 수 없습니다.")
    private String description;

    @Pattern(regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp).*|)$",
            message = "올바른 이미지 URL 형식이 아닙니다.")
    private String profileImageUrl;
}