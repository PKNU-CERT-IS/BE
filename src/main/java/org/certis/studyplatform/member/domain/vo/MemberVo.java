package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Member Response VO Record
 * 불변성이 보장된 읽기 전용 값 객체
 */
public record MemberVo(
        Long id,
        String name,
        String studentNumber,
        String profileImage,
        String grade,
        String role,
        List<String> skills,
        String major,
        String description,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    // 방어적 복사를 위한 생성자
    public MemberVo {
        skills = skills != null ? List.copyOf(skills) : List.of();
    }

    // 편의 메서드
    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }
}