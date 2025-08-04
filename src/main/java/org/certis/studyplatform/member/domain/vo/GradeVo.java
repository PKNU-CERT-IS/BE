package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.Set;

/**
 * 학년 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 유효한 학년 값 관리
 */
@Embeddable
public record GradeVo(String value) {
    
    private static final Set<String> VALID_GRADES = Set.of(
        "1학년", "2학년", "3학년", "4학년", 
        "석사", "박사", "수료생"
    );
    
    public GradeVo {
        validateGrade(value);
    }
    
    public static GradeVo of(String grade) {
        return new GradeVo(grade);
    }
    
    /**
     * 학년 비즈니스 규칙 검증
     */
    private static void validateGrade(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_GRADE, 
                    "학년은 필수입니다");
        }
        
        String trimmedGrade = grade.trim();
        
        if (!VALID_GRADES.contains(trimmedGrade)) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_GRADE, 
                    "유효하지 않은 학년입니다: " + trimmedGrade + 
                    ". 허용된 값: " + String.join(", ", VALID_GRADES));
        }
    }
} 