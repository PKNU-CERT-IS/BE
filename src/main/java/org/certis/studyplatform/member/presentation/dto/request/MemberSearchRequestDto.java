package org.certis.studyplatform.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.certis.studyplatform.member.domain.MemberRole;

/**
 * 회원 검색 요청 DTO
 * 
 * 타입/정적분석만 수행 - 기본적인 null 체크와 타입 검증
 * 비즈니스 규칙 검증은 Domain 레이어에서 수행
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSearchRequestDto {
    
    /**
     * 학년 필터 (선택적)
     * 예: "1학년", "2학년", "석사", "박사" 등
     */
    private String grade;
    
    /**
     * 역할 필터 (선택적)
     * 예: "개발자", "디자이너", "기획자" 등
     */
    private MemberRole role;
    
    /**
     * 검색 키워드 (선택적)
     * 이름, 전공, 기술 스택에서 검색
     */
    private String keyword;
    
    /**
     * 기본 필드 검증 - 모든 필터가 비어있는지 확인
     */
    public boolean hasAnyFilter() {
        return (grade != null && !grade.trim().isEmpty()) ||
               (role != null)||
               (keyword != null && !keyword.trim().isEmpty());
    }
    
    /**
     * 안전한 문자열 반환 (null 처리)
     */
    public String getSafeGrade() {
        return grade != null ? grade.trim() : null;
    }
    
    public MemberRole getSafeRole() {
        return role != null ? role : null;
    }
    
    public String getSafeKeyword() {
        return keyword != null ? keyword.trim() : null;
    }
} 