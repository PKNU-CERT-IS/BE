package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * A Value Object representing a summary of a member for query purposes.
 * The Domain's query repository will return this object instead of the full Member entity
 * to the Application layer, enforcing a strict separation of concerns.
 */
public record MemberSummaryVo(
        Long id,
        String name,
        String studentNumber,
        String grade,
        String role,
        String major,
        String description,
        List<String> skills,
        String profileImage,
        ZonedDateTime createdAt
) {}
