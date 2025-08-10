package org.certis.studyplatform.member.domain;

import lombok.Getter;

@Getter
public enum MemberRole {
    ADMIN("관리자"),
    CHAIRMAN("회장"),
    VICECHAIRMAN("부회장"),
    STAFF("임원진"),
    LEADER("스터디/프로젝트장"),
    PLAYER("참여자"),
    UPSOLVER("업솔버"),
    NORMAL("일반회원"), // 기존 데이터 호환성을 위해 추가
    NONE("미지정");

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

    /**
     * 문자열로부터 MemberRole 찾기 (안전한 변환)
     */
    public static MemberRole fromString(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return NONE;
        }

        String trimmed = roleString.trim().toUpperCase();
        
        try {
            return MemberRole.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // 기존 데이터 호환성을 위한 매핑
            return switch (trimmed) {
                case "MEMBER" -> NORMAL; // 기존 MEMBER를 NORMAL로 매핑
                case "PENDING" -> NONE; // 기존 PENDING을 NONE으로 매핑
                default -> NONE;
            };
        }
    }

    public boolean isAdmin(){
        return this == ADMIN;
    }

    public boolean isStaff(){
        return this == STAFF || this == CHAIRMAN || this == VICECHAIRMAN;
    }

    public boolean isLeader(){
        return this == LEADER;
    }

    public boolean isChairman(){
        return this == CHAIRMAN;
    }

    public boolean isViceChairman(){
        return this == VICECHAIRMAN;
    }

    public boolean isPlayer(){
        return this == PLAYER;
    }

    public boolean isUpSolver(){
        return this == UPSOLVER;
    }

    public boolean isNormal(){
        return this == NORMAL;
    }
}
