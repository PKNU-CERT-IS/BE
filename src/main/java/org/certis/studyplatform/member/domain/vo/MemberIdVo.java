package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;

/**
 * 회원 ID Value Object
 * 
 * 순수한 래핑 역할만 수행 - 검증은 DTO 레이어에서 완료
 * 타입 안전성과 도메인 개념 캡슐화에만 집중
 */
@Embeddable
public record MemberIdVo(Long value) {
    
    public static MemberIdVo of(Long id) {
        return new MemberIdVo(id);
    }
} 