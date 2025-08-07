package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.StudentNumberVo;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.infrastructure.mapper.MemberMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.dao.DataIntegrityViolationException;
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
 * - 통합 매퍼 시스템 사용: MemberMapper
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MemberCommandRepositoryImpl implements MemberCommandRepository {
    
    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;
    
    @Override
    public Member save(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("저장할 회원 정보가 null입니다");
        }

        log.info("Command Infrastructure: Saving member: {}", member.getStudentNumber().value());

        try {
            // 새로 생성하는 경우 중복 체크
            if (member.getId() == null) {
                if (memberJpaRepository.existsByStudentNumber(member.getStudentNumber().value())) {
                    log.warn("Duplicate student number attempted: {}", member.getStudentNumber().value());
                    // throw DomainException.duplicateStudentNumber(member.getStudentNumber().value());
                }
            }

            // 통합 매퍼 사용: Domain → Entity 변환
            MemberEntity memberEntity = memberMapper.toEntity(member);

            // JPA 저장
            MemberEntity savedEntity = memberJpaRepository.save(memberEntity);
            log.info("Member saved successfully with ID: {}", savedEntity.getId());

            // 통합 매퍼 사용: Entity → Domain 변환하여 반환
            return memberMapper.toDomain(savedEntity);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving member: {}", e.getMessage());
            // 적절한 비즈니스 예외로 변환
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while saving member: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void deleteById(MemberIdVo memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("삭제할 회원 ID가 null입니다");
        }
        
        log.info("Command Infrastructure: Deleting member by ID: {}", memberId.value());
        
        try {
            if (!memberJpaRepository.existsById(memberId.value())) {
                throw new IllegalArgumentException("삭제하려는 회원이 존재하지 않습니다: " + memberId.value());
            }
            
            memberJpaRepository.deleteById(memberId.value());
            log.info("Member deleted successfully: {}", memberId.value());
            
        } catch (Exception e) {
            log.error("Error deleting member {}: {}", memberId.value(), e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Optional<Member> findById(MemberIdVo memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("조회할 회원 ID가 null입니다");
        }
        
        log.info("Command Infrastructure: Finding member by ID: {}", memberId.value());
        
        try {
            Optional<MemberEntity> memberEntity = memberJpaRepository.findById(memberId.value());
            
            if (memberEntity.isPresent()) {
                // 통합 매퍼 사용: Entity → Domain 변환
                Member member = memberMapper.toDomain(memberEntity.get());
                log.info("Member found: {}", member.getStudentNumber().value());
                return Optional.of(member);
            } else {
                log.info("Member not found: {}", memberId.value());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error finding member {}: {}", memberId.value(), e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean existsByStudentNumber(StudentNumberVo studentNumber) {
        if (studentNumber == null) {
            throw new IllegalArgumentException("조회할 학번이 null입니다");
        }
        
        log.info("Command Infrastructure: Checking existence by student number: {}", studentNumber.value());
        
        try {
            boolean exists = memberJpaRepository.existsByStudentNumber(studentNumber.value());
            log.info("Student number {} exists: {}", studentNumber.value(), exists);
            return exists;
            
        } catch (Exception e) {
            log.error("Error checking student number existence {}: {}", studentNumber.value(), e.getMessage());
            throw e;
        }
    }
    

}