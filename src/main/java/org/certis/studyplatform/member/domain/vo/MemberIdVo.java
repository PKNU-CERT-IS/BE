package org.certis.studyplatform.member.domain.vo;

/**
 * 회원 ID Value Object
 * 
 * 회원 식별자를 담는 불변 객체
 * 도메인 전반에서 회원을 식별하는 용도로 사용
 */
public record MemberIdVo(Long value) {
    
    public MemberIdVo {
        if (value == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다");
        }
    }
    
    /**
     * 팩토리 메서드
     */
    public static MemberIdVo of(Long value) {
        return new MemberIdVo(value);
    }
    
    /**
     * Long 타입으로 변환
     */
    public Long toLong() {
        return value;
    }
} 