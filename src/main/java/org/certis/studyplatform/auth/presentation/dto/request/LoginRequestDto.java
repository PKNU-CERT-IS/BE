package org.certis.studyplatform.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class LoginRequestDto {

    @NotBlank(message = "계정번호는 필수입니다")
    private String accountNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
//    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;
}