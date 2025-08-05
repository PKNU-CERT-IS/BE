package org.certis.studyplatform.member.domain;

import lombok.Getter;

@Getter
public enum MemberRole {
    ADMIN("관리자"),
    MEMBER("회원"),
    LEADER("스터디/프로젝트장"),
    STAFF("임원진"),
    PENDING("승인 대기");

    private final String description;

    MemberRole(String description){
        this.description = description;
    }

    // ROLE_ 접두사 를 추가하는 함수 (Spring Security용)
    public String toAuthorityString(){
        return "ROLE_" + this.name();
    }

    // ROLE_ 접두사를 제거 (Role 반환)
    public static MemberRole fromAuthorityString(String authorityString){
        String normalizedRole =  authorityString.replace("ROLE_","");
        return MemberRole.valueOf(normalizedRole);
    }

    public boolean isAdmin(){
        return this == ADMIN;
    }

    public boolean isStaff(){
        return this == STAFF;
    }

    public boolean isLeader(){
        return this == LEADER;
    }
}
