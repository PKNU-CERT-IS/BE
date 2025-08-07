package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.Profile;

import java.time.ZonedDateTime;

/**
 * 내 프로필 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProfileInfoResponseDto {
    private Long memberId;
    private String name;
    private String description;
    private String profileImageUrl;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Domain Entity → Response DTO 변환
     */
    public static ProfileInfoResponseDto from(Profile profile) {
        return ProfileInfoResponseDto.builder()
                .memberId(profile.getMemberId())
                .name(profile.getName())
                .description(profile.getDescription())
                .profileImageUrl(profile.getProfileImageValue())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
