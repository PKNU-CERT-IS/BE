package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;

/**
 * 역할 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 역할 정보 형식과 규칙을 도메인에서 관리
 */
@Embeddable
public record RoleVo(MemberRole role) {
    
    public RoleVo {
        validateRole(role);
    }
    
    public static RoleVo of(MemberRole role) {
        return new RoleVo(role);
    }
    
    /**
     * 역할 비즈니스 규칙 검증
     */
    private static void validateRole(MemberRole role) {
        if (role == null) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_ROLE, 
                    "역할은 필수입니다");
        }
    }
} 