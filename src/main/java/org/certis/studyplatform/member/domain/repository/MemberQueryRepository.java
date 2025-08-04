package org.certis.studyplatform.member.domain.repository;

import org.certis.studyplatform.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Member Query Repository (Read Operations Only)
 * 
 * CQRS Query 측면의 Repository 인터페이스 (Domain Layer)
 * jOOQ를 통한 최적화된 읽기 작업만 제공
 * 
 * 책임:
 * - 복잡한 조회 쿼리 수행 (R Operations)
 * - Domain Entity 반환
 * - 성능 최적화된 읽기 전용 쿼리
 * - 페이징 및 필터링 지원
 * 
 * 특징:
 * - Domain Layer에 위치하며 Domain Entity 반환
 * - Infrastructure Layer에서 구현
 */
public interface MemberQueryRepository {
    
    /**
     * 회원 상세 정보 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 상세 정보 (Optional)
     */
    Optional<Member> findMemberById(Long memberId);
    
    /**
     * 구조화된 검색 조건을 사용한 회원 목록 조회 (페이징)
     * 
     * @param searchCriteria 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 회원 정보
     */
    Page<Member> findMembers(MemberSearchCriteria searchCriteria, Pageable pageable);
    
    /**
     * 필터링된 회원 목록 조회 (페이징) - Legacy 호환성
     * 
     * @param nameFilter 이름 필터 (Like 검색)
     * @param roleFilter 역할 필터 (정확 일치)
     * @param gradeFilter 학년 필터 (정확 일치)
     * @param skillFilter 기술 필터 (Like 검색)
     * @param pageable 페이징 정보
     * @return 페이징된 회원 정보
     */
    default Page<Member> findMembers(String nameFilter, 
                                    String roleFilter, 
                                    String gradeFilter, 
                                    String skillFilter, 
                                    Pageable pageable) {
        MemberSearchCriteria criteria = MemberSearchCriteria.builder()
                .keyword(nameFilter)
                .role(roleFilter)
                .grade(gradeFilter)
                .skill(skillFilter)
                .build();
        
        return findMembers(criteria, pageable);
    }
    
    /**
     * 역할별 회원 수 집계
     * 
     * @return 역할별 회원 수 맵
     */
    // Map<String, Long> countMembersByRole();
    
    /**
     * 학년별 회원 수 집계
     * 
     * @return 학년별 회원 수 맵
     */
    // Map<String, Long> countMembersByGrade();
    
    /**
     * 최근 가입한 회원 목록 조회
     * 
     * @param limit 조회 제한 수
     * @return 최근 가입 회원 목록
     */
    // List<Member> findRecentMembers(int limit);
    
    /**
     * 회원 검색 조건 클래스
     */
    record MemberSearchCriteria(
        String keyword,     // 이름, 전공, 기술 스택에서 검색
        String grade,       // 학년 필터 (정확 일치)
        String role,        // 역할 필터 (정확 일치)
        String skill        // 특정 기술 필터 (포함 검색)
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String keyword;
            private String grade;
            private String role;
            private String skill;
            
            public Builder keyword(String keyword) {
                this.keyword = keyword;
                return this;
            }
            
            public Builder grade(String grade) {
                this.grade = grade;
                return this;
            }
            
            public Builder role(String role) {
                this.role = role;
                return this;
            }
            
            public Builder skill(String skill) {
                this.skill = skill;
                return this;
            }
            
            public MemberSearchCriteria build() {
                return new MemberSearchCriteria(keyword, grade, role, skill);
            }
        }
        
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