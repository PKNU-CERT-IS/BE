package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.infrastructure.jpa.MemberJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 기반 Member 데이터 액세스 구현체
 * 
 * Infrastructure Layer의 실제 JPA 데이터 액세스 구현
 * Command 작업과 단순 Query 작업을 담당
 * 
 * 책임:
 * - JPA를 통한 CRUD 작업
 * - Entity ↔ Domain 변환
 * - 데이터베이스 예외 처리
 * - 데이터 무결성 검증
 */
@Repository("memberJpaDataAccessRepository")
@RequiredArgsConstructor
@Slf4j
public class MemberJpaDataAccessRepository implements MemberDataAccessRepository {
    
    private final MemberJpaRepository memberJpaRepository;
    
    // Command Operations
    
    @Override
    public Member saveMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("저장할 회원 정보가 null입니다");
        }

        log.info("Saving member: {}", member.getStudentNumber().value());

        try {
            // 새로 생성하는 경우 중복 체크
            if (member.getId() == null) {
                log.debug("Creating new member: {}", member.getStudentNumber().value());

                // 학번 중복 체크
                Optional<MemberEntity> existingByStudentNumber =
                    memberJpaRepository.findByStudentNumber(member.getStudentNumber().value());

                if (existingByStudentNumber.isPresent()) {
                    log.warn("Duplicate student number attempted: {}", member.getStudentNumber().value());
//                    throw DomainException.duplicateStudentNumber(member.getStudentNumber().value());
                }
            } else {
                log.debug("Updating existing member: {}", member.getId().value());
            }

            // Domain → Entity 변환
            MemberEntity memberEntity = MemberEntity.fromDomain(member);

            // JPA 저장
            MemberEntity savedEntity = memberJpaRepository.save(memberEntity);
            log.info("Member saved successfully with ID: {}", savedEntity.getId());

            // Entity → Domain 변환하여 반환
            return savedEntity.toDomain();

        } catch (DataIntegrityViolationException e) {
            // DB 제약 조건 위반을 비즈니스 예외로 변환
            log.error("Data integrity violation while saving member: {}", e.getMessage());
//
//            if (e.getMessage().contains("student_number")) {
//                throw DomainException.duplicateStudentNumber(member.getStudentNumber().value());
//            }
//
            throw e;
//            throw InfrastructureException.databaseError("회원 저장", e);

        } catch (Exception e) {
            log.error("Unexpected error while saving member: {}", e.getMessage(), e);
//            throw InfrastructureException.databaseError("회원 저장", e);
            throw e;
        }
    }
    
    @Override
    public void deleteMemberById(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("삭제할 회원 ID가 null입니다");
        }
        
        log.info("Deleting member by ID: {}", memberId);
        
        try {
            // 존재 여부 확인
            if (!memberJpaRepository.existsById(memberId)) {
                throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND, 
                        "삭제하려는 회원이 존재하지 않습니다: " + memberId);
            }
            
            // Soft delete (SQLDelete 어노테이션에 의해 처리됨)
            memberJpaRepository.deleteById(memberId);
            log.info("Member deleted successfully: {}", memberId);
            
        } catch (DomainException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("Error deleting member {}: {}", memberId, e.getMessage());
//            throw InfrastructureException.databaseError("회원 삭제", e);
        }
    }
    
    @Override
    public Optional<Member> findMemberByIdForCommand(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("조회할 회원 ID가 null입니다");
        }
        
        log.debug("Finding member by ID for command: {}", memberId);
        
        try {
            return memberJpaRepository.findById(memberId)
                    .map(MemberEntity::toDomain);
        } catch (Exception e) {
            log.error("Error finding member by ID {}: {}", memberId, e.getMessage());
//            throw InfrastructureException.databaseError("회원 조회", e);
            throw e;
        }
    }
    
    @Override
    public boolean existsByStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            return false;
        }
        
        try {
            return memberJpaRepository.findByStudentNumber(studentNumber).isPresent();
        } catch (Exception e) {
            log.error("Error checking student number existence {}: {}", 
                    studentNumber, e.getMessage());
            return false;
        }
    }
    
    // 이메일 관련 기능은 현재 MemberEntity에 email 필드가 없어서 제외
    
    @Override
    public long countByRole(String role) {
        if (role == null) {
            return 0L;
        }
        
        try {
            return memberJpaRepository.findByRole(role).size();
        } catch (Exception e) {
            log.error("Error counting members by role {}: {}", role, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public long countByGrade(String grade) {
        if (grade == null) {
            return 0L;
        }
        
        try {
            return memberJpaRepository.findByGrade(grade).size();
        } catch (Exception e) {
            log.error("Error counting members by grade {}: {}", grade, e.getMessage());
            return 0L;
        }
    }
    
    // Query Operations (단순한 경우만 JPA로 처리)
    
    @Override
    public Optional<Member> findMemberByIdForQuery(Long memberId) {
        // 단순 조회의 경우 Command와 동일하게 처리
        return findMemberByIdForCommand(memberId);
    }
    
    @Override
    public Page<Member> findMembersByCriteria(MemberSearchCriteria searchCriteria, Pageable pageable) {
        // 복잡한 쿼리는 jOOQ 구현체에서 처리하므로 기본 구현만 제공
        log.warn("Complex query should be handled by jOOQ implementation");
        return new PageImpl<>(java.util.List.of(), pageable, 0);
    }
} 