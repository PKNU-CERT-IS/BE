package org.certis.studyplatform.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청 DTO
 *
 * Clean Architecture Presentation Layer
 * 프로필 정보 수정 요청
 *
 * ✅ 정적 검증만 포함 (타입, null 체크)
 * ✅ 비즈니스 규칙 검증은 Domain VO에서 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDto {

    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
    private String name;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
    private String description;

    @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다.")
    private String profileImage; // profileImageUrl에서 profileImage로 변경
}
