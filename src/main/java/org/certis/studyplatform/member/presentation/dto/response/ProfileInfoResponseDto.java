package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String profileImage;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
