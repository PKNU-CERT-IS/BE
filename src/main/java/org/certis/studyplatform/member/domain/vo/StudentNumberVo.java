package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * 학번 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 학번 형식과 규칙을 도메인에서 관리
 */
@Embeddable
public record StudentNumberVo(String value) {
    
    public StudentNumberVo {
        validateStudentNumber(value);
    }
    
    public static StudentNumberVo of(String studentNumber) {
        return new StudentNumberVo(studentNumber);
    }
    
    /**
     * 학번 비즈니스 규칙 검증
     */
    private static void validateStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_STUDENT_NUMBER, 
                    "학번은 필수입니다");
        }
        
        String trimmedNumber = studentNumber.trim();
        
        if (trimmedNumber.length() < 6 || trimmedNumber.length() > 20) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_STUDENT_NUMBER, 
                    "학번은 6자 이상 20자 이하여야 합니다");
        }
        
        if (!trimmedNumber.matches("^[0-9]+$")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_STUDENT_NUMBER, 
                    "학번은 숫자만 포함할 수 있습니다");
        }
    }
} 