package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequestDto {
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotBlank(message = "학번은 필수입니다")
    @Pattern(regexp = "\\d{8,10}", message = "학번은 8-10자리 숫자여야 합니다")
    private String studentNumber;

    @NotBlank(message = "학년은 필수입니다")
    private String grade;

    @NotBlank(message = "역할은 필수입니다")
    private String role;

    private String major;
}