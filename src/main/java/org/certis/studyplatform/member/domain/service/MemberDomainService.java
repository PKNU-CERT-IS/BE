package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.command.GetMemberTokenInfoQuery;
import org.certis.studyplatform.member.application.object.command.*;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersForAdminQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.mapper.MemberDomainMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Member Domain Service - Command/Query 객체 기반
 *
 * Clean Architecture Domain Layer의 서비스
 * Command/Query 객체를 받아 VO로 변환하여 비즈니스 로직 처리
 *
 * 책임:
 * - Command/Query 객체를 VO로 변환 (Domain 레이어 전용)
 * - VO 생성 시점에서 비즈니스 검증 수행
 * - 복합 도메인 로직 처리 (VO 기반)
 * - Repository 인터페이스를 통한 영속성 추상화 (VO 반환)
 *
 * 특징:
 * - Command/Query 객체를 받아 단방향 데이터 흐름 보장
 * - VO 생성자/팩토리 메서드에서 검증 수행
 * - Infrastructure를 모름 (Repository Interface만 사용)
 * - 순수하게 VO만 다루며 Entity 변환은 Infrastructure 담당
 * - Repository는 VO를 반환하여 Domain은 VO만 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDomainService {

    private final MemberCommandRepository memberCommandRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberDomainMapper memberDomainMapper;

    // ================================================================
    // COMMAND OPERATIONS - Command 객체 기반 (VO만 처리)
    // ================================================================

    /**
     * 회원 생성 (Command 기반) - Enhanced VO-Centric Flow
     * 
     * 데이터 흐름:
     * 1. Command Object (primitive) → VO 변환 (비즈니스 검증 자동 수행)
     * 2. 개별 VO들을 복합 VO로 조합
     * 3. 복합 비즈니스 규칙 검증
     * 4. Repository 호출 (VO 전달 → Infrastructure에서 Entity 변환)
     * 5. 결과 VO 반환 (Infrastructure에서 Entity → VO 변환)
     */
    public MemberCreatedVo createMember(CreateMemberCommand command) {
        log.info("🎯 Domain: Starting member creation with student number: {}", command.studentNumber());

        // ================================================================
        // STEP 1: Command → VO 변환 (비즈니스 검증 자동 수행)
        // 각 VO 생성자에서 개별 필드 검증이 자동으로 수행됨
        // ================================================================
        
        log.debug("📝 Converting Command primitives to VOs with validation...");
        
        // 이름 VO 변환 (길이, 형식, null 검증 자동 수행)
        NameVo nameVo = memberDomainMapper.toNameVo(command.name());
        log.debug("✅ NameVo created: {}", nameVo.value());
        
        // 학번 VO 변환 (길이, 형식, 중복 검증 준비)
        StudentNumberVo studentNumberVo = memberDomainMapper.toStudentNumberVo(command.studentNumber());
        log.debug("✅ StudentNumberVo created: {}", studentNumberVo.value());
        
        // 학년 VO 변환 (유효한 학년 값 검증 자동 수행)
        GradeVo gradeVo = memberDomainMapper.toGradeVo(command.grade());
        log.debug("✅ GradeVo created: {}", gradeVo.value());

        SkillsVo skillsVo = null; // 회원가입에서는 항상 null
        
        // 역할 VO 변환 (길이, 형식 검증 자동 수행)
        RoleVo roleVo = memberDomainMapper.toRoleVo(command.role());
        log.debug("✅ RoleVo created: {}", roleVo.role());
        
        // 전공 VO 변환 (길이, 형식 검증 자동 수행)
        MajorVo majorVo = memberDomainMapper.toMajorVo(command.major());
        log.debug("✅ MajorVo created: {}", majorVo.value());
        
        // 선택적 필드 VO 변환
        EmailVo emailVo = command.email() != null ? 
            memberDomainMapper.toEmailVo(command.email()) : null;
        ProfileImageVo profileImageVo = command.profileImage() != null ? 
            memberDomainMapper.toProfileImageVo(command.profileImage()) : null;
            
        if (emailVo != null) log.debug("✅ EmailVo created: {}", emailVo.value());
        if (profileImageVo != null) log.debug("✅ ProfileImageVo created");

        BirthdayVo birthdayVo = BirthdayVo.of(command.birthday());
        GenderVo genderVo = GenderVo.of(command.gender());

        // ================================================================
        // STEP 2: 개별 VO들을 복합 VO로 조합
        // MemberCreationVo는 회원 생성에 필요한 모든 VO를 포함하는 복합 VO
        // ================================================================
        
        log.debug("🔗 Composing individual VOs into MemberCreationVo...");
        // 수정 필요 (회원가입 전용 팩토리 사용)
        MemberCreationVo creationVo = MemberCreationVo.forRegistration(
                nameVo, studentNumberVo, gradeVo, roleVo, majorVo, birthdayVo, genderVo
        );
        log.debug("✅ MemberCreationVo composed successfully");

        // ================================================================
        // STEP 3: 복합 비즈니스 규칙 검증
        // 개별 VO 검증을 통과한 후, 여러 VO 간의 관계나 외부 상태 검증
        // ================================================================
        
        log.debug("🔍 Validating complex business rules...");
        validateMemberCreation(creationVo);
        log.debug("✅ Complex business rules validation passed");

        // ================================================================
        // STEP 4: Repository 호출 (VO → Infrastructure → Entity 변환)
        // Infrastructure Layer에서 VO를 Entity로 변환하여 영속화
        // ================================================================
        
        log.debug("💾 Calling repository to persist member (VO → Entity)...");
        MemberCreatedVo createdMember = memberCommandRepository.createMember(creationVo);
        log.info("🎉 Domain: Member created successfully with ID: {}", createdMember.id());

        return createdMember;
    }

    /**
     * 회원 정보 수정 (Command 기반) - Enhanced VO-Centric Flow
     * 
     * 데이터 흐름:
     * 1. Command Object (primitive) → VO 변환 (비즈니스 검증 자동 수행)
     * 2. 개별 VO들을 복합 VO로 조합 (Builder Pattern)
     * 3. 복합 비즈니스 규칙 검증
     * 4. Repository 호출 (VO 전달 → Infrastructure에서 Entity 변환)
     * 5. 결과 VO 반환 (Infrastructure에서 Entity → VO 변환)
     */
    public MemberUpdatedVo updateMember(UpdateMemberCommand command) {
        log.info("🔄 Domain: Starting member update with ID: {}", command.id());

        // ================================================================
        // STEP 1: Command → MemberIdVo 변환 (검증 자동 수행)
        // ================================================================
        
        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(command.id());
        log.debug("✅ MemberIdVo created: {}", memberIdVo.value());

        // 기존 회원 존재 확인
        validateMemberExists(memberIdVo);

        // ================================================================
        // STEP 2: Command → VO 변환 (중복 코드 제거)
        // 각 필드를 개별적으로 VO로 변환 후 Builder에 직접 설정
        // ================================================================
        
        log.debug("📝 Converting Command fields to VOs with validation...");
        
        MemberUpdateVo.Builder updateBuilder = MemberUpdateVo.builder();

        // 이름 VO 변환 및 설정 (null-safe)
        if (command.name() != null) {
            NameVo nameVo = memberDomainMapper.toNameVo(command.name());
            updateBuilder.name(nameVo);
            log.debug("✅ NameVo converted and set: {}", nameVo.value());
        }

        // 학년 VO 변환 및 설정 (null-safe)
        if (command.grade() != null) {
            GradeVo gradeVo = memberDomainMapper.toGradeVo(command.grade());
            updateBuilder.grade(gradeVo);
            log.debug("✅ GradeVo converted and set: {}", gradeVo.value());
        }

        // 역할 VO 변환 및 설정 (null-safe)
        if (command.role() != null) {
            RoleVo roleVo = memberDomainMapper.toRoleVo(command.role());
            updateBuilder.role(roleVo);
            log.debug("✅ RoleVo converted and set: {}", roleVo.role());
        }

        // 전공 VO 변환 및 설정 (null-safe)
        if (command.major() != null) {
            MajorVo majorVo = memberDomainMapper.toMajorVo(command.major());
            updateBuilder.major(majorVo);
            log.debug("✅ MajorVo converted and set: {}", majorVo.value());
        }

        // 기술스택 VO 변환 및 설정 (null-safe)
        if (command.skills() != null) {
            SkillsVo skillsVo = memberDomainMapper.toSkillsVo(command.skills());
            updateBuilder.skills(skillsVo);
            log.debug("✅ SkillsVo converted and set with {} skills", skillsVo.values().size());
        }

        // 설명 설정 (primitive type)
        updateBuilder.description(command.description());
        if (command.description() != null) {
            log.debug("✅ Description set: {}", command.description());
        }

        // ================================================================
        // STEP 3: 복합 VO 생성 및 검증
        // ================================================================
        
        log.debug("🔗 Building MemberUpdateVo from individual VOs...");
        MemberUpdateVo updateVo = updateBuilder.build();
        log.debug("✅ MemberUpdateVo built successfully");

        // 업데이트할 내용이 있는지 확인
        if (!updateVo.hasAnyUpdate()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "수정할 정보가 없습니다");
        }

        // ================================================================
        // STEP 4: 복합 비즈니스 규칙 검증
        // ================================================================
        
        log.debug("🔍 Validating complex business rules for update...");
        validateMemberUpdate(updateVo);
        log.debug("✅ Complex business rules validation passed");

        // ================================================================
        // STEP 5: Repository 호출 (VO → Infrastructure → Entity 변환)
        // ================================================================
        
        log.debug("💾 Calling repository to update member (VO → Entity)...");
        MemberUpdatedVo updatedMember = memberCommandRepository.updateMember(memberIdVo, updateVo);
        log.info("🎉 Domain: Member updated successfully with ID: {}", updatedMember.id());

        return updatedMember;
    }

    /**
     * 회원 삭제 (Command 기반)
     * Command 객체를 받아 VO로 변환하며 검증 수행
     */
    public void deleteMember(DeleteMemberCommand command) {
        log.info("Domain: Deleting member with ID: {}", command.id());

        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(command.id());
        validateMemberDeletion(memberIdVo);

        memberCommandRepository.deleteById(memberIdVo);
        log.info("Domain: Member deleted successfully: {}", command.id());
    }

    // ================================================================
    // QUERY OPERATIONS - Query 객체 기반 (VO만 처리)
    // ================================================================

    /**
     * 회원 상세 조회 (Query 기반) - Enhanced VO-Centric Flow
     * 
     * 데이터 흐름:
     * 1. Query Object (primitive) → VO 변환 (검증 자동 수행)
     * 2. Repository 호출 (VO 전달 → Infrastructure에서 Entity 조회)
     * 3. 결과 VO 반환 (Infrastructure에서 Entity → VO 변환)
     */
    public MemberVo getMemberVo(GetMemberByIdQuery query) {
        log.info("🔍 Domain: Starting member lookup with ID: {}", query.id());

        // ================================================================
        // STEP 1: Query → VO 변환 (검증 자동 수행)
        // Query Object의 primitive 값을 검증된 VO로 변환
        // ================================================================
        
        log.debug("📝 Converting Query primitive to VO with validation...");
        
        // ID VO 변환 (양수 검증, null 검증 자동 수행)
        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(query.id());
        log.debug("✅ MemberIdVo created: {}", memberIdVo.value());

        // ================================================================
        // STEP 2: Repository 호출 (VO → Infrastructure → Entity 조회)
        // Infrastructure Layer에서 VO를 사용하여 Entity 조회 후 VO로 변환
        // ================================================================
        
        log.debug("🔍 Calling repository to find member (VO → Entity lookup → VO)...");
        
        return memberQueryRepository.findById(memberIdVo)
                .map(memberVo -> {
                    log.debug("✅ Member found and converted to VO");
                    log.debug("📤 Returning MemberVo: name={}, studentNumber={}", 
                            memberVo.name(), memberVo.studentNumber());
                    log.info("🎉 Domain: Member lookup completed successfully for ID: {}", query.id());
                    return memberVo;
                })
                .orElseThrow(() -> {
                    log.warn("❌ Member not found with ID: {}", query.id());
                    return new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                            "회원을 찾을 수 없습니다: " + query.id());
                });
    }

    /**
     * 회원 검색 (Query 기반)
     * Repository는 VO를 반환
     */
    public Page<MemberSummaryVo> searchMemberVos(SearchMembersQuery query) {
        log.info("Domain: Searching member VOs with criteria: {}", query.keyword());
        return memberQueryRepository.searchMembers(query);
    }

    /**
     * 전체 회원 조회 (Query 기반)
     * Repository는 VO를 반환
     */
    public Page<MemberSummaryVo> getAllMemberVos(GetMembersQuery query) {
        log.info("Domain: Getting all member VOs with pagination");
        return memberQueryRepository.findAll(query);
    }

    // ================================================================
    // BUSINESS LOGIC VALIDATION METHODS - VO 기반
    // ================================================================

    /**
     * 회원 생성 검증 (VO 기반)
     */
    private void validateMemberCreation(MemberCreationVo creationVo) {
        // VO 레벨 검증은 이미 각 VO 생성자에서 수행됨

        // 학번 중복 체크
        if (isStudentNumberDuplicated(creationVo.studentNumber())) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_DUPLICATE_STUDENT_NUMBER,
                    "이미 존재하는 학번입니다: " + creationVo.studentNumber().value());
        }

        // 추가 비즈니스 규칙 검증
        validateBusinessRules(creationVo);
    }

    /**
     * 회원 수정 검증 (VO 기반)
     */
    private void validateMemberUpdate(MemberUpdateVo updateVo) {
        // 각 VO의 검증은 이미 생성 시점에서 수행됨

        // 이름이 업데이트되는 경우 추가 비즈니스 규칙 검증
        if (updateVo.name() != null) {
            validateNameBusinessRules(updateVo.name());
        }

        // 학년이 업데이트되는 경우 추가 비즈니스 규칙 검증
        if (updateVo.grade() != null) {
            validateGradeBusinessRules(updateVo.grade());
        }

        // 역할이 업데이트되는 경우 추가 비즈니스 규칙 검증
        if (updateVo.role() != null) {
            validateRoleBusinessRules(updateVo.role());
        }

        // 전공이 업데이트되는 경우 추가 비즈니스 규칙 검증
        if (updateVo.major() != null) {
            validateMajorBusinessRules(updateVo.major());
        }

        // 기술 스택이 업데이트되는 경우 추가 비즈니스 규칙 검증
        if (updateVo.skills() != null) {
            validateSkillsBusinessRules(updateVo.skills());
        }
    }

    /**
     * 회원 삭제 검증
     */
    private void validateMemberDeletion(MemberIdVo memberIdVo) {
        if (memberIdVo == null || memberIdVo.value() == null) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "회원 ID는 필수입니다");
        }

        // 회원 존재 확인
        validateMemberExists(memberIdVo);

        // 추가 삭제 제약 조건 검증 (예: 진행 중인 프로젝트가 있는지 등)
        validateDeletionConstraints(memberIdVo);
    }

    /**
     * 회원 존재 여부 확인
     */
    private void validateMemberExists(MemberIdVo memberIdVo) {
        if (!memberCommandRepository.existsById(memberIdVo)) {
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                    "회원을 찾을 수 없습니다: " + memberIdVo.value());
        }
    }

    /**
     * 학번 중복 확인
     */
    private boolean isStudentNumberDuplicated(StudentNumberVo studentNumber) {
        return memberCommandRepository.existsByStudentNumber(studentNumber);
    }

    // ================================================================
    // BUSINESS RULES VALIDATION - VO 기반
    // ================================================================

    private void validateBusinessRules(MemberCreationVo creationVo) {
        // 학년과 역할 간의 비즈니스 규칙 검증
        validateGradeRoleConsistency(creationVo.grade(), creationVo.role());

        // 전공과 기술 스택 간의 비즈니스 규칙 검증
        if (creationVo.skills() != null) {
            validateMajorSkillsConsistency(creationVo.major(), creationVo.skills());
        }
    }

    private void validateNameBusinessRules(NameVo name) {
        // 이름 관련 비즈니스 규칙 (VO 검증 외의 추가 규칙)
        if (name.value().matches(".*\\d.*")) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "이름에는 숫자가 포함될 수 없습니다");
        }
    }

    private void validateGradeBusinessRules(GradeVo grade) {
        // 학년 관련 비즈니스 규칙
        // 추가 규칙이 필요한 경우 여기에 구현
    }

    private void validateRoleBusinessRules(RoleVo role) {
        // 역할 관련 비즈니스 규칙
        // 추가 규칙이 필요한 경우 여기에 구현
    }

    private void validateMajorBusinessRules(MajorVo major) {
        // 전공 관련 비즈니스 규칙
        // 추가 규칙이 필요한 경우 여기에 구현
    }

    private void validateSkillsBusinessRules(SkillsVo skills) {
        // 기술 스택 관련 비즈니스 규칙
        if (skills.values().size() > 10) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "기술 스택은 최대 10개까지만 등록할 수 있습니다");
        }
    }

    private void validateGradeRoleConsistency(GradeVo grade, RoleVo role) {
        // 예: 1학년은 팀장이 될 수 없다는 규칙
        if ("1".equals(grade.value()) && "LEADER".equals(role.role())) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_NAME,
                    "1학년은 팀장 역할을 할 수 없습니다");
        }
    }

    private void validateMajorSkillsConsistency(MajorVo major, SkillsVo skills) {
        // 전공과 기술 스택 간의 일관성 검증
        // 예: 특정 전공에 맞지 않는 기술 스택 체크
        // 구체적인 비즈니스 규칙에 따라 구현
    }

    private void validateDeletionConstraints(MemberIdVo memberIdVo) {
        // 삭제 제약 조건 검증
        // 예: 진행 중인 프로젝트가 있는지, 팀장인지 등
        // 구체적인 비즈니스 규칙에 따라 구현
    }

    // 어드민 필드
    public AdminMemberUpdateResultVo updateMemberAdminFields(UpdateMemberAdminFieldsCommand command) {
        log.info("Domain: 관리자 필드 변경 시작 - 실행자: {}, 대상자: {}",
                command.executorId(), command.targetMemberId());

        MemberIdVo targetIdVo = new MemberIdVo(command.targetMemberId());
        MemberVo targetMember = memberQueryRepository.findById(targetIdVo)
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_DOMAIN_NOT_FOUND));

        MemberUpdateVo.Builder updateBuilder = MemberUpdateVo.builder();

        if(command.newRole() != null) {
            RoleVo currentRoleVo = RoleVo.of(targetMember.role());
            RoleVo newRoleVo = RoleVo.of(command.newRole());
            RoleVo executorRoleVo = RoleVo.of(command.executorRole());

            // ✅ 핵심: 권한 검증 (실행자가 대상자의 권한을 변경할 수 있는가?)
            executorRoleVo.validateCanManageRole(currentRoleVo, newRoleVo);
            updateBuilder.role(newRoleVo);
        }

        if (command.newGrade() != null && !command.newGrade().trim().isEmpty()) {
            GradeVo newGradeVo = GradeVo.of(command.newGrade());
            updateBuilder.grade(newGradeVo);
        }

        MemberUpdateVo memberUpdateVo = updateBuilder.build();

        memberCommandRepository.updateMember(targetIdVo, memberUpdateVo);
        log.info("Domain: 관리자 필드 변경 완료 - 대상자: {}", command.targetMemberId());


        return AdminMemberUpdateResultVo.of(
                command.targetMemberId(),
                memberUpdateVo.getRoleValue(),
                memberUpdateVo.getGradeValue()
        );
    }

    public MemberTokenInfoVo getMemberTokenInfoVo(GetMemberTokenInfoQuery query) {
        log.info("🔍 Domain: Starting member token info lookup with ID: {}", query.memberId());

        // ID VO 변환 (양수 검증, null 검증 자동 수행)
        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(query.memberId());

        return memberQueryRepository.findTokenInfoById(query.memberId())
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                        "회원 정보를 찾을 수 없습니다: " + query.memberId()));
    }

    public List<MemberSearchForAdminVo> searchMembersForAdmin(SearchMembersForAdminQuery query) {
        log.info("🔍 Domain: Searching members for admin with keyword={}", query.keyword());

        // STEP 1: Query → VO 변환
        SearchKeywordVo keywordVo = SearchKeywordVo.of(query.keyword());

        // STEP 2: Repository 호출 (VO 전달)
        List<MemberSearchForAdminVo> voList = memberQueryRepository.searchMembersForAdmin(keywordVo);

        log.info("✅ Domain: Found {} members for keyword={}", voList.size(), keywordVo.value());
        return voList;
    }

    /**
     * 유예기간 부여 (Command 기반)
     */
    public void grantGracePeriod(UpdateGracePeriodCommand command) {
        log.info("🕒 Domain: Granting grace period - memberId={}, until={}",
                command.memberId(), command.gracePeriod());

        // STEP 1: Command → VO 변환
        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(command.memberId());
        GracePeriodVo gracePeriodVo = GracePeriodVo.of(command.gracePeriod());

        validateMemberExists(memberIdVo);

        // STEP 2: Repository 호출 (VO 전달)
        memberCommandRepository.updateGracePeriod(memberIdVo, gracePeriodVo);

        log.info("✅ Domain: Grace period granted successfully - memberId={}", command.memberId());
    }

    /**
     * 패널티 부여 (Command 기반)
     */
    public void assignPenalty(UpdatePenaltyCommand command) {
        log.info("⚠️ Domain: Assigning penalty - memberId={}, points={}",
                command.memberId(), command.penaltyPoints());

        // STEP 1: Command → VO 변환
        MemberIdVo memberIdVo = memberDomainMapper.toMemberIdVo(command.memberId());
        PenaltyPointsVo penaltyPointsVo = PenaltyPointsVo.of(command.penaltyPoints());

        validateMemberExists(memberIdVo);

        // STEP 2: Repository 호출 (VO 전달)
        memberCommandRepository.updatePenalty(memberIdVo, penaltyPointsVo);

        log.info("✅ Domain: Penalty assigned successfully - memberId={}, points={}",
                command.memberId(), command.penaltyPoints());
    }

    public void applyGracePeriodForGrantingPenalties() {
        OffsetDateTime now = OffsetDateTime.now();
        List<MemberWithPenaltyVo> expiredUpsolvers = memberQueryRepository.findExpiredUpsolvers(now);

        for (MemberWithPenaltyVo member : expiredUpsolvers) {
            Long newPoints = (member.penaltyPoints() != null ? member.penaltyPoints() : 0) + 1;
            PenaltyPointsVo penaltyVo = PenaltyPointsVo.of(newPoints);

            OffsetDateTime nextGrace = now.plusWeeks(2)
                    .toLocalDate()
                    .atStartOfDay()
                    .atOffset(ZoneOffset.UTC);
            GracePeriodVo graceVo = GracePeriodVo.of(nextGrace);

            memberCommandRepository.updatePenalty(member.memberId(), penaltyVo);
            memberCommandRepository.updateGracePeriod(member.memberId(), graceVo);
        }
    }
}