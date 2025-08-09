package org.certis.studyplatform.member.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.repository.MemberQueryRepository;
import org.certis.studyplatform.member.domain.repository.MemberQueryRepository.MemberSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Member Query Service
 * 
 * CQRS Query 측면의 통합 서비스 (Application Layer)
 * 모든 회원 관련 읽기 작업을 담당
 * jOOQ 기반 MemberQueryRepository 사용
 * 
 * 책임:
 * - Query 처리 오케스트레이션
 * - 읽기 전용 트랜잭션 관리
 * - 성능 최적화된 읽기 작업
 * - Cross-cutting concerns (로깅, 캐싱, 보안 등)
 * 
 * Clean Architecture 의존성:
 * Application → Domain (Repository Interface) → Infrastructure (Repository Impl)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberQueryService {
    
    private final MemberQueryRepository memberQueryRepository;
    
    /**
     * 회원 검색
     * 
     * @param keyword 검색 키워드
     * @param grade 학년 필터
     * @param role 역할 필터
     * @param pageable 페이징 정보
     * @return 검색된 회원 목록과 페이징 정보
     */
    public Page<Member> searchMembers(String keyword, String grade, MemberRole role, Pageable pageable) {
        log.info("Application: Searching members with criteria - keyword: {}, grade: {}, role: {}", 
                keyword, grade, role);
        
        // 검색 조건 구성
        MemberSearchCriteria criteria = MemberSearchCriteria.builder()
                .keyword(keyword)
                .grade(grade)
                .role(role)
                .build();
        
        // 검색 조건이 모두 비어있으면 예외 발생
        if (criteria.isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_PRESENTATION_INVALID_REQUEST, 
                    "최소 하나의 검색 조건이 필요합니다");
        }
        
        // jOOQ를 통한 최적화된 검색 작업
        Page<Member> searchResult = memberQueryRepository.findMembers(criteria, pageable);
        
        log.info("Application: Found {} members out of {} total", 
                searchResult.getNumberOfElements(), 
                searchResult.getTotalElements());
        
        return searchResult;
    }
    
    /**
     * 회원 상세 정보 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 상세 정보
     */
    public Member getMemberById(Long memberId) {
        log.info("Application: Getting member with ID: {}", memberId);
        
        // jOOQ를 통한 최적화된 read 작업
        Member member = memberQueryRepository.findMemberById(memberId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND, 
                        "회원을 찾을 수 없습니다: " + memberId));
        
        log.info("Application: Member found: {}", member.getName().value());
        
        return member;
    }
    
    /**
     * 필터링된 회원 목록 조회
     * 
     * @param nameFilter 이름 필터
     * @param roleFilter 역할 필터
     * @param gradeFilter 학년 필터
     * @param skillFilter 기술 필터
     * @param pageable 페이징 정보
     * @return 페이징된 회원 목록
     */
    public Page<Member> findMembers(String nameFilter, MemberRole roleFilter,
                                   String gradeFilter, String skillFilter, 
                                   Pageable pageable) {
        log.info("Application: Finding members with filters - name: {}, role: {}, grade: {}, skill: {}", 
                nameFilter, roleFilter, gradeFilter, skillFilter);
        
        // jOOQ를 통한 최적화된 read 작업
        Page<Member> members = memberQueryRepository.findMembers(
            nameFilter, roleFilter, gradeFilter, skillFilter, pageable
        );
        
        log.info("Application: Found {} members", members.getTotalElements());
        
        return members;
    }
    
    /**
     * 전체 회원 목록 조회
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 회원 목록
     */
    public Page<Member> findAllMembers(Pageable pageable) {
        log.info("Application: Finding all members");
        
        return findMembers(null, null, null, null, pageable);
    }
} 