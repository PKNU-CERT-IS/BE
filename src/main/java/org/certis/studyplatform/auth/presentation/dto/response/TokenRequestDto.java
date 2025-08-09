package org.certis.studyplatform.auth.presentation.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDto {

    @NotBlank(message = "액세스 토큰은 필수입니다")
    private String accessToken;

    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "리프레시 토큰 만료시간은 필수입니다.")
    private LocalDateTime refreshExpiredAt;

    @NotNull(message = "역할(role)은 필수입니다")
    private MemberRole role;
}
