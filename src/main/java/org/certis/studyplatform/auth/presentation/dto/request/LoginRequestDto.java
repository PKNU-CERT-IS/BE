package org.certis.studyplatform.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public  class LoginRequestDto {

    @NotBlank(message = "계정번호는 필수입니다")
    private String accountNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    public boolean hasRequiredFields() {
        return accountNumber != null && !accountNumber.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }
}