package org.certis.studyplatform.member.application.object.query;

import org.certis.studyplatform.member.domain.MemberRole;

public record SearchMembersWithContactQuery(
        String search,     // 이름, 전공, 기술스택 검색
        String grade,            // 학년 필터
        MemberRole role          // 역할 필터
) {

    public static SearchMembersWithContactQuery of(String search, String grade, MemberRole role) {
        return new SearchMembersWithContactQuery(search, grade, role);
    }
}
