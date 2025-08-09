package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
