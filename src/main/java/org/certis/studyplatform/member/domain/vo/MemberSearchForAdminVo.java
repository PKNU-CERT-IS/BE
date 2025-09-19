package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

public record MemberSearchForAdminVo(
        MemberIdVo memberId,
        String name,
        MemberRole role,
        String major,
        String studentNumber,
        List<String> activeStudies,
        List<String> activeProjects,
        Long penaltyPoints,
        OffsetDateTime gracePeriod,
        String grade,
        String gender,
        OffsetDateTime birthday,
        String phoneNumber,
        String email,
        OffsetDateTime createdAt
) {
    /**
     * 유예기간 만료 여부 확인
     */
    public boolean isGracePeriodExpired() {
        return gracePeriod != null && gracePeriod.isBefore(OffsetDateTime.now());
    }
}
