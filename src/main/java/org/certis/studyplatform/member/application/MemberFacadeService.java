package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.MemberCommandService;
import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Member Facade Service
 * 
 * Controller와 Command/Query Service 사이의 복잡성을 숨기고
 * 단순하고 일관된 인터페이스를 제공하는 Facade 패턴 구현
 * 
 * Domain Entity를 사용하여 타입 안전성 보장
 * 
 * 책임:
 * - Command/Query Service 오케스트레이션
 * - 트랜잭션 경계 관리
 * - 비즈니스 워크플로우 조정
 * - Cross-cutting concerns 처리 (로깅, 검증 등)
 * 
 * Clean Architecture:
 * Application Facade → Application Services → Domain Services → Domain Repository Interfaces
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFacadeService {

    // CQRS Services
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    /**
     * 회원 생성
     * 
     * @param name 이름
     * @param studentNumber 학번
     * @param grade 학년
     * @param skills 기술 스택
     * @param role 역할
     * @param major 전공
     * @param description 설명
     * @return 생성된 회원 정보
     */
    @Transactional
    public Member createMember(String name, String studentNumber, String grade, 
                              List<String> skills, MemberRole role, String major, String description) {
        log.info("Creating member via facade - student number: {}", studentNumber);
        
        // Command Service 실행 (JPA write)
        Member createdMember = memberCommandService.createMember(
            name, studentNumber, grade, skills, role, major, description
        );
        
        log.info("Member created via facade - ID: {}", 
                createdMember.getId() != null ? createdMember.getId().value() : "null");
        
        return createdMember;
    }
    
    /**
     * 회원 프로필 수정
     * 
     * @param memberId 회원 ID
     * @param name 새 이름 (nullable)
     * @param profileImage 새 프로필 이미지 (nullable)
     */
    @Transactional
    public void updateMemberProfile(Long memberId, String name, String profileImage) {
        log.info("Updating member profile via facade - ID: {}", memberId);
        
        // Command Service 실행 (JPA write)
        memberCommandService.updateMemberProfile(memberId, name, profileImage);
        
        log.info("Member profile updated via facade - ID: {}", memberId);
    }
    
    /**
     * 회원 기술 스택 수정
     * 
     * @param memberId 회원 ID
     * @param skills 새 기술 스택
     */
    @Transactional
    public void updateMemberSkills(Long memberId, List<String> skills) {
        log.info("Updating member skills via facade - ID: {}", memberId);
        
        // Command Service 실행 (JPA write)
        memberCommandService.updateMemberSkills(memberId, skills);
        
        log.info("Member skills updated via facade - ID: {}", memberId);
    }
    
    /**
     * 회원 역할 수정
     * 
     * @param memberId 회원 ID
     * @param role 새 역할
     */
    @Transactional
    public void updateMemberRole(Long memberId, MemberRole role) {
        log.info("Updating member role via facade - ID: {}", memberId);
        
        // Command Service 실행 (JPA write)
        memberCommandService.updateMemberRole(memberId, role);
        
        log.info("Member role updated via facade - ID: {}", memberId);
    }
    
    /**
     * 회원 삭제
     * 
     * @param memberId 삭제할 회원 ID
     */
    @Transactional
    public void deleteMember(Long memberId) {
        log.info("Deleting member via facade - ID: {}", memberId);
        
        // Command Service 실행 (JPA write)
        memberCommandService.deleteMember(memberId);
        
        log.info("Member deleted via facade - ID: {}", memberId);
    }
    
    /**
     * 회원 상세 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 상세 정보
     */
    public Member getMemberById(Long memberId) {
        log.info("Getting member via facade - ID: {}", memberId);
        
        // Query Service 실행 (jOOQ read)
        Member member = memberQueryService.getMemberById(memberId);
        
        log.info("Member retrieved via facade - name: {}", member.getName().value());
        
        return member;
    }
    
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
        log.info("Searching members via facade - keyword: {}, grade: {}, role: {}", keyword, grade, role);
        
        // Query Service 실행 (jOOQ read)
        Page<Member> result = memberQueryService.searchMembers(keyword, grade, role, pageable);
        
        log.info("Search completed via facade - found {} results", result.getTotalElements());
        
        return result;
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
        log.info("Finding members via facade with filters - name: {}, role: {}, grade: {}, skill: {}", 
                nameFilter, roleFilter, gradeFilter, skillFilter);
        
        // Query Service 실행 (jOOQ read)
        Page<Member> result = memberQueryService.findMembers(nameFilter, roleFilter, gradeFilter, skillFilter, pageable);
        
        log.info("Find completed via facade - found {} results", result.getTotalElements());
        
        return result;
    }

    /**
     * 전체 회원 목록 조회 (필터 없이)
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 회원 목록
     */
    public Page<Member> findAllMembers(Pageable pageable) {
        log.info("Finding all members via facade");
        
        // Query Service 실행 (jOOQ read)
        Page<Member> result = memberQueryService.findAllMembers(pageable);
        
        log.info("Find all completed via facade - found {} results", result.getTotalElements());
        
        return result;
    }
} 