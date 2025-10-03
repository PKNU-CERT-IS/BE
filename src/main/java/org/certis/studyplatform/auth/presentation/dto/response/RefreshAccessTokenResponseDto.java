package org.certis.studyplatform.auth.presentation.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshAccessTokenResponseDto {
    @NotBlank(message = "액세스 토큰은 필수입니다")
    private String accessToken;

    public RefreshAccessTokenResponseDto(String accessToken) {
        this.accessToken = accessToken;
    }
}
