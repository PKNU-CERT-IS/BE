package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

/**
 * 회원 정보 통합 수정 요청 DTO
 *
 * Clean Architecture Presentation Layer
 * 프로필, 기술스택, 기본정보를 하나의 DTO로 통합 처리
 *
 * ✅ 정적 검증만 포함 (타입, null 체크)
 * ✅ 모든 필드는 선택적 (부분 업데이트 지원)
 * ✅ 비즈니스 규칙 검증은 Domain VO에서 수행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
    // 프로필 관련 필드 (선택적)
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]*$", message = "이름은 한글, 영문, 공백만 포함할 수 있습니다.")
    private String name;

    private String profileImage; // 프로필 이미지 (선택적)

    // 기본 정보 필드 (선택적)
    private MemberGrade grade;

    private MemberRole role;

    private String major;

    private String description;

    // 기술 스택 필드 (선택적)
    @Size(min = 1, message = "기술 스택은 최소 1개 이상이어야 합니다")
    private List<@NotBlank(message = "기술 스택 항목은 빈 값일 수 없습니다") String> skills;

    /**
     * 업데이트할 필드가 있는지 확인
     */
    public boolean hasUpdateFields() {
        return name != null || profileImage != null || grade != null ||
               role != null || major != null || description != null ||
               (skills != null && !skills.isEmpty());
    }

    /**
     * 프로필 관련 필드만 업데이트하는지 확인
     */
    public boolean isProfileUpdateOnly() {
        return (name != null || profileImage != null) &&
               grade == null && role == null && major == null &&
               description == null && (skills == null || skills.isEmpty());
    }

    /**
     * 기술 스택만 업데이트하는지 확인
     */
    public boolean isSkillsUpdateOnly() {
        return name == null && profileImage == null && grade == null &&
               role == null && major == null && description == null &&
               skills != null && !skills.isEmpty();
    }

    /**
     * 기본 정보만 업데이트하는지 확인
     */
    public boolean isBasicInfoUpdateOnly() {
        return (grade != null || role != null || major != null || description != null) &&
               name == null && profileImage == null && (skills == null || skills.isEmpty());
    }
}