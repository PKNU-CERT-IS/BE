package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 회원 프로필 업데이트 요청 DTO
 * 
 * 최초 검증 레이어 - Bean Validation으로 기본 입력 검증 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileRequestDto {
    
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]*$", message = "이름은 한글, 영문, 공백만 포함할 수 있습니다")
    private String name; // 선택적 업데이트
    
    @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다")
    @Pattern(regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp).*|)$", 
             message = "올바른 이미지 URL 형식이 아닙니다")
    private String profileImageUrl; // 선택적 업데이트
    
    /**
     * 업데이트할 필드가 하나라도 있는지 확인
     */
    public boolean hasUpdateFields() {
        return (name != null && !name.trim().isEmpty()) || 
               (profileImageUrl != null && !profileImageUrl.trim().isEmpty());
    }
}
