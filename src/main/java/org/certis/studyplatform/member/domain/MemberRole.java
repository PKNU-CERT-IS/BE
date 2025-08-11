package org.certis.studyplatform.member.domain;

import lombok.Getter;

@Getter
public enum MemberRole {
    ADMIN(0, "최고관리자"),
    CHAIRMAN(1, "회장"),
    VICECHAIRMAN(2, "부회장"),
    STAFF(3, "임원진"),
    PLAYER(4, "일반회원"),
    UPSOLVER(4, "문제해결자"), // PLAYER와 동급
    NONE(5, "승인 대기");

    private final int level;
    private final String description;

    MemberRole(int level, String description){
        this.level = level;
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

    // 권한 등급 비교 : 자신과 같거나 자신보다 낮은 level의 사용자의 role을 변경 가능
    public boolean canChangeRole(MemberRole role){
        return role.level > this.level;
    }


    // staff 이상의 관리자 권한인가? (admin 페이지 접근 가능 판별)
    public boolean isStaffOrAbove() {
        return this.level <= STAFF.level;
    }

}
