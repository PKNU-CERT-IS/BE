package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;

public record MemberCreatedVo(
        Long id,
        String name,
        String studentNumber,
        ZonedDateTime createdAt
) {}