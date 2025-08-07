package org.certis.studyplatform.member.application.object.query;

import org.springframework.data.domain.Pageable;

public record GetMemberSummariesQuery(
        String searchKeyword,
        String grade,
        String role,
        Pageable pageable
) {}