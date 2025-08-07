package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.command.UpdateMemberCommand;
import org.certis.studyplatform.member.application.object.command.UpdateProfileCommand;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.mapper.PrimitiveToVoMapper;
import org.certis.studyplatform.member.domain.mapper.VoToDomainMapper;
import org.certis.studyplatform.member.domain.mapper.DomainToVoMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

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
 * - 새로운 매퍼 시스템 사용: PrimitiveToVoMapper, VoToDomainMapper, DomainToVoMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDomainService {

    private final MemberCommandRepository memberCommandRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final PrimitiveToVoMapper primitiveToVoMapper;
    private final VoToDomainMapper voToDomainMapper;
    private final DomainToVoMapper domainToVoMapper;

    // ================================================================
    // COMMAND OPERATIONS - 기존 구조 유지
    // ================================================================

    /**
     * 기존 메서드 시그니처 그대로 유지
     */
    public MemberCreatedVo createMember(String name, String studentNumber, String grade,
                                        List<String> skills, String role, String major, String description) {
        log.info("Domain: Creating member with student number: {}", studentNumber);

        // 새로운 매퍼 사용: primitive → VO → Domain 변환
        NameVo nameVo = primitiveToVoMapper.toNameVo(name);
        StudentNumberVo studentNumberVo = primitiveToVoMapper.toStudentNumberVo(studentNumber);
        GradeVo gradeVo = primitiveToVoMapper.toGradeVo(grade);
        RoleVo roleVo = primitiveToVoMapper.toRoleVo(role);
        MajorVo majorVo = primitiveToVoMapper.toMajorVo(major);
        SkillsVo skillsVo = primitiveToVoMapper.toSkillsVo(skills);

        // VO → Domain Entity 변환
        Member member = voToDomainMapper.toMember(name, studentNumber, grade, skills, role, major);
        member.setDescription(description);

        validateMemberCreation(member);

        Member savedMember = memberCommandRepository.save(member);
        log.info("Domain: Member created successfully with ID: {}", savedMember.getId());

        // 새로운 매퍼 사용: Domain → VO 변환
        return domainToVoMapper.toMemberCreatedVo(savedMember);
    }

    /**
     * 기존 메서드 그대로 유지
     */
    public MemberUpdatedVo updateMember(MemberIdVo memberIdVo, Consumer<Member> updateAction) {
        log.info("Domain: Updating member with ID: {}", memberIdVo.value());

        Member member = findMemberEntityById(memberIdVo.value());
        updateAction.accept(member);
        validateMemberUpdate(member);

        Member savedMember = memberCommandRepository.save(member);
        log.info("Domain: Member updated successfully: {}", savedMember.getId());

        // 새로운 매퍼 사용: Domain → VO 변환
        return domainToVoMapper.toMemberUpdatedVo(savedMember);
    }

    /**
     * 새로운 매퍼를 사용한 업데이트 메서드
     */
    public Member updateMember(MemberIdVo memberIdVo, UpdateMemberCommand command) {
        log.info("Domain: Updating member with ID: {} using command", memberIdVo.value());

        Member member = findMemberEntityById(memberIdVo.value());

        // Command의 각 필드를 개별적으로 업데이트
        if (command.name() != null) {
            member.updateProfile(command.name(), null);
        }
        if (command.grade() != null) {
            member.updateGrade(command.grade());
        }
        if (command.role() != null) {
            member.updateRole(command.role());
        }
        if (command.major() != null) {
            member.updateMajor(command.major());
        }
        if (command.description() != null) {
            member.setDescription(command.description());
        }
        if (command.skills() != null && !command.skills().isEmpty()) {
            member.updateSkills(command.skills());
        }

        validateMemberUpdate(member);

        Member savedMember = memberCommandRepository.save(member);
        log.info("Domain: Member updated successfully: {}", savedMember.getId());

        return savedMember;
    }

    /**
     * 프로필 업데이트 메서드
     */
    public Member updateProfile(MemberIdVo memberIdVo, UpdateProfileCommand command) {
        log.info("Domain: Updating member profile with ID: {}", memberIdVo.value());

        Member member = findMemberEntityById(memberIdVo.value());

        // 프로필 정보 업데이트 - profileImageUrl() 메서드 사용
        member.updateProfile(command.name(), command.profileImageUrl());

        validateMemberUpdate(member);

        Member savedMember = memberCommandRepository.save(member);
        log.info("Domain: Member profile updated successfully: {}", savedMember.getId());

        return savedMember;
    }

    /**
     * 기존 메서드 그대로 유지
     */
    public void deleteMember(MemberIdVo memberIdVo) {
        log.info("Domain: Deleting member with ID: {}", memberIdVo.value());
        validateMemberDeletion(memberIdVo.value());
        memberCommandRepository.deleteById(memberIdVo);
        log.info("Domain: Member deleted successfully: {}", memberIdVo.value());
    }

    // ================================================================
    // QUERY OPERATIONS - 새로운 매퍼 사용
    // ================================================================

    /**
     * 회원 상세 조회
     */
    public MemberVo getMemberVo(GetMemberByIdQuery query) {
        log.info("Domain: Getting member VO with ID: {}", query.id());

        MemberIdVo memberIdVo = primitiveToVoMapper.toMemberIdVo(query.id());
        Member member = memberQueryRepository.findById(memberIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND, 
                        "회원을 찾을 수 없습니다: " + query.id()));

        // 새로운 매퍼 사용: Domain → VO 변환
        return domainToVoMapper.toMemberVo(member);
    }

    /**
     * 회원 검색
     */
    public Page<MemberSummaryVo> searchMemberVos(SearchMembersQuery query) {
        log.info("Domain: Searching member VOs with criteria: {}", query.keyword());

        Page<Member> members = memberQueryRepository.searchMembers(query);

        // 새로운 매퍼 사용: Domain → VO 변환
        return members.map(domainToVoMapper::toMemberSummaryVo);
    }

    /**
     * 전체 회원 조회
     */
    public Page<MemberSummaryVo> getAllMemberVos(GetMembersQuery query) {
        log.info("Domain: Getting all member VOs with pagination");

        Page<Member> members = memberQueryRepository.findAll(query);

        // 새로운 매퍼 사용: Domain → VO 변환
        return members.map(domainToVoMapper::toMemberSummaryVo);
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    private Member findMemberEntityById(Long memberId) {
        MemberIdVo memberIdVo = primitiveToVoMapper.toMemberIdVo(memberId);
        return memberCommandRepository.findById(memberIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND, 
                        "회원을 찾을 수 없습니다: " + memberId));
    }

    private void validateMemberCreation(Member member) {
        // 학번 중복 체크
        if (isStudentNumberDuplicated(member.getStudentNumber())) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_DUPLICATE_STUDENT_NUMBER,
                    "이미 존재하는 학번입니다");
        }

        // 이메일 중복 체크 - Member 클래스에 getEmail 메서드가 없으므로 제거
        // 실제로는 Member 클래스에 email 필드가 있는지 확인 필요
    }

    private void validateMemberUpdate(Member member) {
        // 업데이트 시 필요한 검증 로직
        if (member.getName() == null || member.getName().value().trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "이름은 필수입니다");
        }
    }

    private void validateMemberDeletion(Long memberId) {
        // 삭제 시 필요한 검증 로직
        if (memberId == null) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "회원 ID는 필수입니다");
        }
    }

    private boolean isStudentNumberDuplicated(StudentNumberVo studentNumber) {
        return memberCommandRepository.existsByStudentNumber(studentNumber);
    }


}