package org.certis.studyplatform.member.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Member Detail Response DTO Class (Facade용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetailResponseDto {
    // 회원 기본 정보
    private Long id;
    private String name;
    private String studentNumber;
    private String profileImage;
    private String grade;
    private MemberRole role;
    private List<String> skills;
    private String major;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime updatedAt;

    // 프로필 정보
    private String profileDescription;
    private String profileImageUrl;

    // 블로그 정보
    private List<ProfileBlogResponseDto> recentBlogs;
    private int totalBlogCount;
}
