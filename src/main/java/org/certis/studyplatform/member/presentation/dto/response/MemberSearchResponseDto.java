package org.certis.studyplatform.member.presentation.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 회원 검색 응답 DTO
 *
 * 검색 결과와 페이지네이션 정보 포함
 */
import lombok.Getter;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;


@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class MemberSearchResponseDto {
    private Long id;
    private String name;
    private String profileImage;
    private MemberGrade grade;
    private MemberRole role;
    private List<String> skills;
    private String major;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String email;
    private String githubUrl;
    private String linkedinUrl;

    public static MemberSearchResponseDto of(
            Long id,
            String name,
            String profileImage,
            MemberGrade grade,
            MemberRole role,
            List<String> skills,
            String major,
            String description,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String email,
            String githubUrl,
            String linkedinUrl
    ) {
        return new MemberSearchResponseDto(
                id,
                name,
                profileImage,
                grade,
                role,
                skills,
                major,
                description,
                createdAt,
                updatedAt,
                email,
                githubUrl,
                linkedinUrl
        );
    }
}