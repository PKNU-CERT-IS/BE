package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;

public record MemberUpdatedVo(
        Long id,
        String name,
        String profileImage,
        ZonedDateTime updatedAt
) {}