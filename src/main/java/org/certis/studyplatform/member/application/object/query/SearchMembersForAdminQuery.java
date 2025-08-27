package org.certis.studyplatform.member.application.object.query;

public record SearchMembersForAdminQuery( String keyword
) {
    public static SearchMembersForAdminQuery of(String keyword) {
        return new SearchMembersForAdminQuery(keyword);
    }
}