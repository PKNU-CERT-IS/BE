package org.certis.studyplatform.member.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Member Command Service
 * 
 * CQRS Command 측면의 통합 서비스 (Application Layer)
 * 모든 회원 관련 쓰기 작업을 담당
 * Domain Service를 통해 비즈니스 로직 수행
 * 
 * 책임:
 * - Command 처리 오케스트레이션
 * - 트랜잭션 관리
 * - 도메인 이벤트 발행
 * - Primitive 파라미터 → Domain 변환
 * - Cross-cutting concerns (로깅, 보안, 캐싱 등)
 * 
 * Clean Architecture 의존성:
 * Application → Domain (MemberDomainService)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCommandService {
    
    private final MemberDomainService memberDomainService;
    private final ApplicationEventPublisher eventPublisher;
    
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
                              List<String> skills, String role, String major, String description) {
        log.info("Application: Creating member with student number: {}", studentNumber);
        
        // 1. Domain Service를 통한 비즈니스 로직 수행
        Member savedMember = memberDomainService.createMember(
            name, studentNumber, grade, skills, role, major, description
        );
        
        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MemberCreatedEvent(savedMember.getId()));
        
        // 3. Application 레벨 부가 작업
        // - 이메일 발송
        // - 캐시 처리
        // - 외부 시스템 연동 등
        
        log.info("Application: Member created successfully with ID: {}", 
                savedMember.getId() != null ? savedMember.getId().value() : "null");
        
        return savedMember;
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
        log.info("Application: Updating member profile with ID: {}", memberId);
        
        MemberIdVo memberIdVo = new MemberIdVo(memberId);
        
        // 1. Domain Service를 통한 비즈니스 로직 수행
        memberDomainService.updateMember(memberIdVo, member -> {
            member.updateProfile(
                name != null ? name : member.getNameValue(),
                profileImage != null ? profileImage : 
                    (member.getProfileImage() != null ? member.getProfileImage().value() : null)
            );
        });
        
        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MemberProfileUpdatedEvent(memberIdVo));
        
        log.info("Application: Member profile updated successfully: {}", memberId);
    }
    
    /**
     * 회원 기술 스택 수정
     * 
     * @param memberId 회원 ID
     * @param skills 새 기술 스택
     */
    @Transactional
    public void updateMemberSkills(Long memberId, List<String> skills) {
        log.info("Application: Updating member skills with ID: {}", memberId);
        
        MemberIdVo memberIdVo = new MemberIdVo(memberId);
        
        // 1. Domain Service를 통한 비즈니스 로직 수행
        memberDomainService.updateMember(memberIdVo, member -> {
            member.updateSkills(skills);
        });
        
        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MemberSkillsUpdatedEvent(memberIdVo));
        
        log.info("Application: Member skills updated successfully: {}", memberId);
    }
    
    /**
     * 회원 역할 수정
     * 
     * @param memberId 회원 ID
     * @param role 새 역할
     */
    @Transactional
    public void updateMemberRole(Long memberId, MemberRole role) {
        log.info("Application: Updating member role with ID: {}", memberId);
        
        MemberIdVo memberIdVo = new MemberIdVo(memberId);
        
        // 1. Domain Service를 통한 비즈니스 로직 수행
        memberDomainService.updateMember(memberIdVo, member -> {
            member.updateRole(role);
        });
        
        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MemberRoleUpdatedEvent(memberIdVo));
        
        log.info("Application: Member role updated successfully: {}", memberId);
    }
    
    /**
     * 회원 삭제
     * 
     * @param memberId 삭제할 회원 ID
     */
    @Transactional
    public void deleteMember(Long memberId) {
        log.info("Application: Deleting member with ID: {}", memberId);
        
        MemberIdVo memberIdVo = new MemberIdVo(memberId);
        
        // 1. Domain Service를 통한 비즈니스 로직 수행
        memberDomainService.deleteMember(memberIdVo);
        
        // 2. 도메인 이벤트 발행 (향후 구현)
        // eventPublisher.publishEvent(new MemberDeletedEvent(memberIdVo));
        
        // 3. Application 레벨 부가 작업
        // - 관련 데이터 정리
        // - 캐시 무효화
        // - 외부 시스템 연동 등
        
        log.info("Application: Member deleted successfully: {}", memberId);
    }
} 