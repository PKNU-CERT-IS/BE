package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.StudentNumberVo;
import org.certis.studyplatform.member.domain.repository.MemberCommandRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Member Domain Service
 *
 * Clean Architecture Domain Layer의 서비스
 * 순수한 비즈니스 로직과 도메인 규칙을 처리
 *
 * 책임:
 * - 복합 도메인 로직 처리
 * - 도메인 규칙 검증
 * - 도메인 객체 간 상호작용 조정
 * - Repository 인터페이스를 통한 영속성 추상화
 *
 * 특징:
 * - Infrastructure를 모름 (Repository Interface만 사용)
 * - 순수한 비즈니스 로직에만 집중
 * - Application Service가 이 서비스를 호출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDomainService {

    private final MemberCommandRepository memberCommandRepository;

    /**
     * 회원 생성 (도메인 로직 포함)
     *
     * @param name 이름
     * @param studentNumber 학번
     * @param grade 학년
     * @param skills 기술 스택
     * @param role 역할
     * @param major 전공
     * @param description 설명
     * @return 생성된 회원 도메인 객체
     */
    public Member createMember(String name, String studentNumber, String grade,
                               List<String> skills, MemberRole role, String major, String description) {
        log.info("Domain: Creating member with student number: {}", studentNumber);

        // 1. 도메인 객체 생성
        Member member = Member.create(name, studentNumber, grade, skills, role, major);
        member.setDescription(description);

        // 2. 도메인 규칙 검증 (Repository를 통한 비즈니스 규칙 체크)
        validateMemberCreation(member);

        // 3. 영속화
        Member savedMember = memberCommandRepository.save(member);

        log.info("Domain: Member created successfully with ID: {}", savedMember.getId());
        return savedMember;
    }

    /**
     * 회원 정보 수정 (도메인 로직 포함)
     *
     * @param memberId 회원 ID
     * @param updateAction 업데이트 액션 (함수형 인터페이스)
     * @return 수정된 회원 도메인 객체
     */
    public Member updateMember(MemberIdVo memberId, MemberUpdateAction updateAction) {
        log.info("Domain: Updating member with ID: {}", memberId.value());

        // 1. 기존 회원 조회
        Member member = findMemberById(memberId);

        // 2. 도메인 로직을 통한 업데이트
        updateAction.apply(member);

        // 3. 도메인 규칙 검증
        validateMemberUpdate(member);

        // 4. 영속화
        Member savedMember = memberCommandRepository.save(member);

        log.info("Domain: Member updated successfully: {}", memberId.value());
        return savedMember;
    }

    /**
     * 회원 삭제 (도메인 로직 포함)
     *
     * @param memberId 삭제할 회원 ID
     */
    public void deleteMember(MemberIdVo memberId) {
        log.info("Domain: Deleting member with ID: {}", memberId.value());

        // 1. 삭제 전 도메인 규칙 검증
        validateMemberDeletion(memberId);

        // 2. 삭제 실행
        memberCommandRepository.deleteById(memberId);

        log.info("Domain: Member deleted successfully: {}", memberId.value());
    }

    /**
     * 회원 ID로 조회 (도메인 서비스용)
     *
     * @param memberId 회원 ID
     * @return 회원 도메인 객체
     */
    public Member findMemberById(MemberIdVo memberId) {
        return memberCommandRepository.findById(memberId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                        "회원을 찾을 수 없습니다: " + memberId.value()));
    }

    /**
     * 학번 중복 체크
     *
     * @param studentNumber 확인할 학번
     * @return 중복 여부
     */
    public boolean isStudentNumberDuplicated(StudentNumberVo studentNumber) {
        return memberCommandRepository.existsByStudentNumber(studentNumber);
    }

    /**
     * 이메일 중복 체크
     *
     * @param email 확인할 이메일
     * @return 중복 여부
     */
    public boolean isEmailDuplicated(String email) {
        return memberCommandRepository.existsByEmail(email);
    }

    // ================================================================
    // PRIVATE VALIDATION METHODS (도메인 규칙 검증)
    // ================================================================

    private void validateMemberCreation(Member member) {
        // 학번 중복 체크
        if (isStudentNumberDuplicated(member.getStudentNumber())) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_DUPLICATE_STUDENT_NUMBER,
                    "이미 존재하는 학번입니다: " + member.getStudentNumber().value());
        }

//        // 이메일 중복 체크 (이메일이 있는 경우)
//        if (member.getEmail() != null && isEmailDuplicated(member.getEmail().value())) {
//            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_DUPLICATE_EMAIL,
//                    "이미 존재하는 이메일입니다: " + member.getEmail().value());
//        }

        // 기타 비즈니스 규칙 검증...
    }

    private void validateMemberUpdate(Member member) {
        // 업데이트 시 필요한 도메인 규칙 검증
        // 예: 특정 역할 변경 권한, 상태 전환 규칙 등
    }

    private void validateMemberDeletion(MemberIdVo memberId) {
        // 삭제 시 필요한 도메인 규칙 검증
        // 예: 진행 중인 프로젝트가 있는지, 리더 역할인지 등

        // 실제로 존재하는지 확인
        if (!memberCommandRepository.findById(memberId).isPresent()) {
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                    "삭제하려는 회원이 존재하지 않습니다: " + memberId.value());
        }
    }

    /**
     * 회원 업데이트를 위한 함수형 인터페이스
     */
    @FunctionalInterface
    public interface MemberUpdateAction {
        void apply(Member member);
    }
}