package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberSkillsRequestDto {
    @NotNull(message = "스킬 목록은 필수입니다")
    private List<String> skills;
}
