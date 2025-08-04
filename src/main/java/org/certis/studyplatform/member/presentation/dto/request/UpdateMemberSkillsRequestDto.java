package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 회원 기술 스택 업데이트 요청 DTO
 * 
 * 최초 검증 레이어 - Bean Validation으로 기본 입력 검증 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberSkillsRequestDto {
    
    @NotNull(message = "기술 스택은 필수입니다")
    @Size(min = 1, max = 20, message = "기술 스택은 1개 이상 20개 이하여야 합니다")
    @Valid
    private List<@NotBlank(message = "기술 스택 항목은 빈 값일 수 없습니다") 
                 @Size(min = 1, max = 50, message = "기술 스택 항목은 1자 이상 50자 이하여야 합니다") String> skills;
    
    /**
     * 유효한 기술 스택이 있는지 확인
     */
    public boolean hasValidSkills() {
        return skills != null && !skills.isEmpty() && 
               skills.stream().allMatch(skill -> skill != null && !skill.trim().isEmpty());
    }
}
