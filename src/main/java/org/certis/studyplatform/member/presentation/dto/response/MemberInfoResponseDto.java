package org.certis.studyplatform.member.presentation.dto.response;

import org.certis.studyplatform.member.domain.Member;
import lombok.*;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.ZonedDateTime;
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
public class MemberInfoResponseDto {
    private Long id;
    private String name;
    private String description;
    private String studentNumber;
    private String profileImage;
    private String grade;
    private MemberRole role;
    private List<String> skills;
    private String major;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Domain Member로부터 DTO 생성
     */
    public static MemberInfoResponseDto fromDomain(Member member) {
        return MemberInfoResponseDto.builder()
                .id(member.getId() != null ? member.getId().value() : null)
                .name(member.getName().value())
                .description(member.getDescription())
                .studentNumber(member.getStudentNumber().value())
                .grade(member.getGrade().value())
                .role(member.getRole().role())
                .major(member.getMajor().value())
                .skills(member.getSkills().values())
                .profileImage(member.getProfileImage() != null ? member.getProfileImage().value() : null)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}