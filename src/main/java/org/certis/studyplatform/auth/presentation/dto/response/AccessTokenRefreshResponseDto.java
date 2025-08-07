package org.certis.studyplatform.auth.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenRefreshResponseDto {
    private String accessToken;

    public boolean hasRequiredFields() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }
}
