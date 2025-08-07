package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

/**
 * 회원 생성 요청 DTO
 * 
 * 타입/정적분석만 수행 - 기본적인 null 체크와 타입 검증
 * 비즈니스 규칙 검증은 Domain 레이어에서 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequestDto {
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "학번은 필수입니다")
    private String studentNumber;
    
    @NotBlank(message = "학년은 필수입니다")
    private String grade;
    
    @NotBlank(message = "역할은 필수입니다")
    private MemberRole role;
    
    @NotNull(message = "기술 스택은 필수입니다")
    @Size(min = 1, message = "기술 스택은 최소 1개 이상이어야 합니다")
    private List<@NotBlank(message = "기술 스택 항목은 빈 값일 수 없습니다") String> skills;
    
    @NotBlank(message = "전공은 필수입니다")
    private String major;
    
    // 선택적 필드 - 기본 타입 검증만
    private String description;
    private String email;
    
    /**
     * 기본 타입 검증 통과 확인
     */
    public boolean hasRequiredFields() {
        return name != null && !name.trim().isEmpty() &&
               studentNumber != null && !studentNumber.trim().isEmpty() &&
               grade != null && !grade.trim().isEmpty() &&
               role != null &&
               skills != null && !skills.isEmpty() &&
               major != null && !major.trim().isEmpty();
    }
}