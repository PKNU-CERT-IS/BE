package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

public record MemberWithContactVo(
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

        // 연락처 정보
        String email,
        String githubUrl,
        String linkedinUrl
) {
    public static MemberWithContactVo of(
            Long id, String name, String profileImage, MemberGrade grade, MemberRole role,
            List<String> skills, String major, String description,
            OffsetDateTime createdAt, OffsetDateTime updatedAt,
            String email, String githubUrl, String linkedinUrl) {
        return new MemberWithContactVo(
                id, name, profileImage, grade, role, skills, major, description,
                createdAt, updatedAt, email, githubUrl, linkedinUrl
        );
    }
}
