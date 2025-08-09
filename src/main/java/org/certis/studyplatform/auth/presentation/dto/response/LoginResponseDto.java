package org.certis.studyplatform.auth.presentation.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    @NotBlank(message = "액세스 토큰은 필수입니다")
    private String accessToken;

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "역할(role)은 필수입니다")
    private MemberRole role;
}
