package org.certis.studyplatform.member.application.object.query;

import org.springframework.data.domain.Pageable;

// TODO: 이름, 전공, 기술스택으로 검색
public record SearchMembersQuery(
        String keyword,

        // TODO: 필터 종류 붙이기
        String grade,
        String role,

        Pageable pageable
) {}