package org.certis.studyplatform.member.infrastructure.persistence;

import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Member 데이터 액세스 Repository Interface
 * 
 * Infrastructure Layer에서 실제 데이터베이스 접근을 담당하는 interface
 * JPA, jOOQ 등 다양한 데이터 액세스 기술의 추상화 레이어
 * 
 * 책임:
 * - 순수한 데이터 CRUD 작업
 * - 데이터베이스 특화 쿼리 실행
 * - Entity ↔ Domain 변환
 * - 데이터 무결성 보장
 */
public interface MemberDataAccessRepository {
    
    // Command Operations (JPA 기반)
    
    /**
     * 회원 데이터 저장 (생성/수정)
     */
    Member saveMember(Member member);
    
    /**
     * 회원 데이터 삭제 (Soft Delete)
     */
    void deleteMemberById(Long memberId);
    
    /**
     * ID로 회원 조회 (Command용 - 단순 조회)
     */
    Optional<Member> findMemberByIdForCommand(Long memberId);
    
    /**
     * 학번 존재 여부 확인
     */
    boolean existsByStudentNumber(String studentNumber);
    
    // 이메일 관련 기능은 현재 MemberEntity에 email 필드가 없어서 제외
    
    /**
     * 역할별 회원 수 집계
     */
    long countByRole(String role);
    
    /**
     * 학년별 회원 수 집계
     */
    long countByGrade(String grade);
    
    // Query Operations (jOOQ 기반)
    
    /**
     * ID로 회원 상세 조회 (Query용 - 최적화된 조회)
     */
    Optional<Member> findMemberByIdForQuery(Long memberId);
    
    /**
     * 검색 조건을 통한 회원 목록 조회 (페이징)
     */
    Page<Member> findMembersByCriteria(MemberSearchCriteria searchCriteria, Pageable pageable);
    
    /**
     * 검색 조건 DTO
     */
    record MemberSearchCriteria(
        String keyword,     // 이름, 전공, 기술 스택에서 검색
        String grade,       // 학년 필터 (정확 일치)
        String role,        // 역할 필터 (정확 일치)
        String skill        // 특정 기술 필터 (포함 검색)
    ) {
        /**
         * 검색 조건이 비어있는지 확인
         */
        public boolean isEmpty() {
            return (keyword == null || keyword.trim().isEmpty()) &&
                   (grade == null || grade.trim().isEmpty()) &&
                   (role == null || role.trim().isEmpty()) &&
                   (skill == null || skill.trim().isEmpty());
        }
        
        /**
         * 안전한 값 반환 (null과 공백 처리)
         */
        public String getSafeKeyword() {
            return keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null;
        }
        
        public String getSafeGrade() {
            return grade != null && !grade.trim().isEmpty() ? grade.trim() : null;
        }
        
        public String getSafeRole() {
            return role != null && !role.trim().isEmpty() ? role.trim() : null;
        }
        
        public String getSafeSkill() {
            return skill != null && !skill.trim().isEmpty() ? skill.trim() : null;
        }
    }
} 