package org.certis.studyplatform.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotBlank(message = "계정번호는 필수입니다")
    private String accountNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotBlank(message = "학번은 필수입니다")
    private String studentNumber;

    @NotBlank(message = "학년은 필수입니다")
    private String grade;

    @NotBlank(message = "전공은 필수입니다")
    private String major;

    @NotBlank(message = "전화번호는 필수입니다")
    private String phoneNumber;

    @NotBlank(message = "이메은 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String email;

    @NotNull(message = "생년월일은 필수입니다")
    private OffsetDateTime birthday;

    @NotBlank(message = "성별은 필수입니다")
    private String gender;
}