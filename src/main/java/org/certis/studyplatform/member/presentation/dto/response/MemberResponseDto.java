package org.certis.studyplatform.member.presentation.dto.response;

import org.certis.studyplatform.member.domain.Member;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Member Information Response DTO
 * 
 * Presentation Layer의 응답 DTO
 * API 응답에 사용되는 회원 정보
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {
    private Long id;
    private String name;
    private String description;
    private String studentNumber;
    private String profileImage;
    private String grade;
    private String role;
    private List<String> skills;
    private String major;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}