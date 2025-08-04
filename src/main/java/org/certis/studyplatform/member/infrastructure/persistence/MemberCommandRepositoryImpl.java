package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.repository.MemberCommandRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Member Command Repository 구현체 (Infrastructure Layer)
 * 
 * MemberCommandRepository 인터페이스의 JPA 기반 구현체
 * Command 패턴에 따른 쓰기 작업 전용
 * 
 * 책임:
 * - JPA를 통한 Command 작업 (CUD Operations)
 * - 트랜잭션 관리 및 데이터 무결성 보장
 * - Command 실행을 위한 최소한의 읽기 작업
 * - 비즈니스 규칙 검증 및 데이터 무결성 처리
 * 
 * CQRS 패턴:
 * - Command 전용: JPA 사용으로 트랜잭션과 데이터 무결성에 최적화
 */
@Repository
@Primary
@Slf4j
public class MemberCommandRepositoryImpl implements MemberCommandRepository {
    
    private final MemberDataAccessRepository jpaDataAccess;
    
    public MemberCommandRepositoryImpl(@Qualifier("memberJpaDataAccessRepository") MemberDataAccessRepository jpaDataAccess) {
        this.jpaDataAccess = jpaDataAccess;
    }
    
    @Override
    public Member save(Member member) {
        log.info("Command Infrastructure: Saving member through JPA");
        return jpaDataAccess.saveMember(member);
    }
    
    @Override
    public void deleteById(MemberIdVo memberId) {
        log.info("Command Infrastructure: Deleting member through JPA: {}", memberId.value());
        jpaDataAccess.deleteMemberById(memberId.value());
    }
    
    @Override
    public Optional<Member> findById(MemberIdVo memberId) {
        log.debug("Command Infrastructure: Finding member by ID through JPA: {}", memberId.value());
        return jpaDataAccess.findMemberByIdForCommand(memberId.value());
    }
    
    @Override
    public boolean existsByStudentNumber(StudentNumberVo studentNumber) {
        if (studentNumber == null) {
            return false;
        }
        log.debug("Command Infrastructure: Checking student number existence through JPA: {}", 
                studentNumber.value());
        return jpaDataAccess.existsByStudentNumber(studentNumber.value());
    }
    
    @Override
    public boolean existsByEmail(String email) {
        log.warn("Email functionality is not implemented yet - MemberEntity doesn't have email field");
        return false;
    }
} 