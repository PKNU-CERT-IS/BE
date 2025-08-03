package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * 역할 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 역할 정보 형식과 규칙을 도메인에서 관리
 */
@Embeddable
public record RoleVo(String value) {
    
    public RoleVo {
        validateRole(value);
    }
    
    public static RoleVo of(String role) {
        return new RoleVo(role);
    }
    
    /**
     * 역할 비즈니스 규칙 검증
     */
    private static void validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_ROLE, 
                    "역할은 필수입니다");
        }
        
        String trimmedRole = role.trim();
        
        if (trimmedRole.length() < 2 || trimmedRole.length() > 100) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_ROLE, 
                    "역할은 2자 이상 100자 이하여야 합니다");
        }
        
        // 역할명에는 기본적인 문자와 일부 특수문자만 허용
        if (!trimmedRole.matches("^[가-힣a-zA-Z0-9\\s\\-_/()]+$")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_ROLE, 
                    "역할은 한글, 영문, 숫자, 공백, 하이픈, 밑줄, 슬래시, 괄호만 포함할 수 있습니다");
        }
    }
} 