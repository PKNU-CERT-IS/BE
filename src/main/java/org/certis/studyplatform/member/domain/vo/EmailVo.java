package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * 이메일 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 이메일 형식과 규칙을 도메인에서 관리
 */
@Embeddable
public record EmailVo(String value) {
    
    public EmailVo {
        validateEmail(value);
    }
    
    public static EmailVo of(String email) {
        return new EmailVo(email);
    }
    
    /**
     * 이메일 비즈니스 규칙 검증
     */
    private static void validateEmail(String email) {
        if (email == null) {
            return; // 선택적 필드이므로 null 허용
        }
        
        String trimmedEmail = email.trim();
        
        if (trimmedEmail.isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_EMAIL, 
                    "이메일은 빈 값일 수 없습니다");
        }
        
        if (trimmedEmail.length() > 100) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_EMAIL, 
                    "이메일은 100자 이하여야 합니다");
        }
        
        if (!trimmedEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_EMAIL, 
                    "올바른 이메일 형식이 아닙니다");
        }
    }
} 