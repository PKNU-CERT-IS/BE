package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;

public record MemberWithPenaltyVo(
        MemberIdVo memberId,
        MemberRole role,
        OffsetDateTime gracePeriod,
        Long penaltyPoints
) {}
