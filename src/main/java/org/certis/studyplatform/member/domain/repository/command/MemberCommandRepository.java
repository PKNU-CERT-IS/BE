package org.certis.studyplatform.member.domain.repository.command;

import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.StudentNumberVo;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;

import java.util.Optional;

/**
 * Member Command Repository (Write Operations Only)
 * 
 * CQRS Command 측면의 Repository
 * JPA를 통한 쓰기 작업 및 Command 실행에 필요한 최소한의 읽기 작업만 제공
 * 
 * 책임:
 * - 회원 생성/수정/삭제 (CUD Operations)
 * - Command 실행을 위한 최소한의 존재성 확인
 * - 데이터 무결성 및 비즈니스 규칙 검증
 */
public interface MemberCommandRepository {
    
    /**
     * 회원 저장 (생성/수정)
     * 
     * @param member 저장할 회원 도메인 객체
     * @return 저장된 회원 도메인 객체
     */
    Member save(Member member);
    
    /**
     * 회원 삭제
     * 
     * @param memberId 삭제할 회원 ID
     */
    void deleteById(MemberIdVo memberId);
    
    /**
     * 회원 ID로 조회 (Command 실행을 위한 최소한의 읽기)
     * 
     * 용도: Update Command에서 기존 회원 조회
     * 
     * @param memberId 회원 ID
     * @return 회원 도메인 객체 (Optional)
     */
    Optional<Member> findById(MemberIdVo memberId);
    
    /**
     * 학번 중복 체크 (Command 실행을 위한 존재성 확인)
     * 
     * 용도: Create/Update Command에서 학번 중복 검증
     * 
     * @param studentNumber 확인할 학번
     * @return 중복 여부 (true: 중복됨, false: 중복되지 않음)
     */
    boolean existsByStudentNumber(StudentNumberVo studentNumber);
    

} 