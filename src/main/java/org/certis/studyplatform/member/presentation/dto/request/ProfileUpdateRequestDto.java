package org.certis.studyplatform.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import org.certis.studyplatform.member.domain.MemberGrade;

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

    private String name;

    private String description;

    private String profileImage; // profileImageUrl에서 profileImage로 변경

    // 추가 필드들 (선택적 갱신)
    private String major;
    private OffsetDateTime birthday;
    private String phoneNumber;
    private String studentNumber;
    private List<String> skills;
    private MemberGrade grade;
    private String email;
    private String githubUrl;
    private String linkedinUrl;
}
