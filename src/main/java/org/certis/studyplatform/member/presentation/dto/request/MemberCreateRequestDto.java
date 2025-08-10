package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 회원 생성 요청 DTO -> 회원 생성 요청은 auth에 넣고 이 dto는 특별한 관리자 생성시 넣도록 합시다.
 * 
 * 타입/정적분석만 수행 - 기본적인 null 체크와 타입 검증
 * 비즈니스 규칙 검증은 Domain 레이어에서 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequestDto {
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "학번은 필수입니다")
    private String studentNumber;
    
    @NotBlank(message = "학년은 필수입니다")
    private String grade;
    
    @NotBlank(message = "역할은 필수입니다")
    private MemberRole role;
    
    @NotNull(message = "기술 스택은 필수입니다")
    private List<@NotBlank(message = "기술 스택 항목은 빈 값일 수 없습니다") String> skills;
    
    @NotBlank(message = "전공은 필수입니다")
    private String major;

    @NotNull(message = "생년월일은 필수입니다")
    private OffsetDateTime birthday;

    @NotBlank(message = "성별은 필수입니다")
    private String gender;

    // 선택적 필드 - 기본 타입 검증만
    private String profileImage;
    private String description;
    private String email;

}