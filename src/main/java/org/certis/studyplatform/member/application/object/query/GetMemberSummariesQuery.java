package org.certis.studyplatform.member.application.object.query;

import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.data.domain.Pageable;

public record GetMemberSummariesQuery(
        String searchKeyword,
        String grade,
        MemberRole role,
        Pageable pageable
) {}