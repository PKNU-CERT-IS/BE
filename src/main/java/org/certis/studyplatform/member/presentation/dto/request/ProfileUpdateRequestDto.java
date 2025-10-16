package org.certis.studyplatform.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    // 프론트엔드에서 FileReader로 변형된 이미지 데이터 (Base64 또는 바이너리)
    private String profileImageData;
    
    // 기존 이미지 URL (수정 시 기존 이미지 유지용)
    private String profileImage;

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
