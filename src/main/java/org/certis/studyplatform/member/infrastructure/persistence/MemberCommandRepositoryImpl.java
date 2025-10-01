package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberPenaltyEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberPenaltyJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Member Command Repository 구현체 (Infrastructure Layer) - VO 기반
 *
 * JPA 기반 Command 작업 전용 구현체 (VO 기반)
 *
 * 책임:
 * - JPA를 통한 Command 작업 (CUD Operations) - VO 기반
 * - VO ↔ Entity 변환 (MemberInfrastructureMapper 사용)
 * - 트랜잭션 관리 및 데이터 무결성 보장
 * - Command 실행을 위한 최소한의 읽기 작업
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberCommandRepositoryImpl implements MemberCommandRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberPenaltyJpaRepository memberPenaltyJpaRepository;
    private final MemberInfrastructureMapper memberInfrastructureMapper;

    @Override
    @Transactional
    public MemberCreatedVo createMember(MemberCreationVo memberCreationVo) {
        log.info("🏗️ Infrastructure: Starting member creation with student number: {}",
                memberCreationVo.studentNumber().value());

        try {
            // ================================================================
            // STEP 1: VO → Entity 변환 (Infrastructure Layer의 핵심 책임)
            // Domain에서 전달받은 VO를 JPA Entity로 변환
            // ================================================================

            log.debug("🔄 Converting MemberCreationVo to MemberEntity...");
            log.debug("📋 Input VO details:");
            log.debug("  - Name: {}", memberCreationVo.getNameValue());
            log.debug("  - Student Number: {}", memberCreationVo.getStudentNumberValue());
            log.debug("  - Grade: {}", memberCreationVo.getGradeValue());
            log.debug("  - Role: {}", memberCreationVo.getRoleValue());
            log.debug("  - Major: {}", memberCreationVo.getMajorValue());
            log.debug("  - Skills: {} items", memberCreationVo.getSkillsValues().size());
            log.debug("  - Email: {}", memberCreationVo.getEmailValue());
            log.debug("  - Profile Image: {}", memberCreationVo.getProfileImageValue() != null ? "Present" : "None");

            // VO → Entity 변환 (매퍼에 위임)
            MemberEntity entityToSave = memberInfrastructureMapper.toEntity(memberCreationVo);
            log.debug("✅ VO → Entity conversion completed");

            // ================================================================
            // STEP 2: JPA를 통한 Entity 영속화
            // 변환된 Entity를 데이터베이스에 저장
            // ================================================================

            log.debug("💾 Persisting MemberEntity to database...");
            MemberEntity savedEntity = memberJpaRepository.save(entityToSave);
            log.debug("✅ Entity persisted with ID: {}", savedEntity.getId());

            // ================================================================
            // STEP 3: Entity → VO 변환 (결과 반환용)
            // 저장된 Entity를 다시 VO로 변환하여 Domain Layer로 반환
            // ================================================================

            log.debug("🔄 Converting saved MemberEntity to MemberCreatedVo...");
            MemberCreatedVo result = memberInfrastructureMapper.toMemberCreatedVo(savedEntity);
            log.debug("✅ Entity → VO conversion completed");
            log.debug("📤 Returning MemberCreatedVo with ID: {}", result.id().value());

            log.info("🎉 Infrastructure: Member created successfully with ID: {}", result.id().value());
            return result;

        } catch (Exception e) {
            log.error("❌ Unexpected error during member creation", e);
            throw new InfrastructureException(ExceptionStatus.MEMBER_INFRASTRUCTURE_DATABASE_ERROR,
                    "회원 생성 중 예상치 못한 오류가 발생했습니다", e);
        }
    }

    @Override
    @Transactional
    public MemberUpdatedVo updateMember(MemberIdVo memberId, MemberUpdateVo memberUpdateVo) {
        log.info("🔄 Infrastructure: Starting member update with ID: {}", memberId.value());

        try {
            // ================================================================
            // STEP 1: 기존 Entity 조회
            // ================================================================

            log.debug("🔍 Finding existing MemberEntity with ID: {}", memberId.value());
            MemberEntity existingEntity = memberJpaRepository.findById(memberId.value())
                    .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                            "회원을 찾을 수 없습니다: " + memberId.value()));
            log.debug("✅ Existing MemberEntity found");

            // ================================================================
            // STEP 2: VO → Entity 업데이트 변환
            // MemberUpdateVo의 정보를 사용하여 기존 Entity 업데이트
            // ================================================================

            log.debug("🔄 Updating MemberEntity using MemberUpdateVo...");
            log.debug("📋 Update details:");
            if (memberUpdateVo.hasNameUpdate()) log.debug("  - Name: {} → {}", existingEntity.getName(), memberUpdateVo.getNameValue());
            if (memberUpdateVo.hasGradeUpdate()) log.debug("  - Grade: {} → {}", existingEntity.getGrade(), memberUpdateVo.getGradeValue());
            if (memberUpdateVo.hasRoleUpdate()) log.debug("  - Role: {} → {}", existingEntity.getRole(), memberUpdateVo.getRoleValue());
            if (memberUpdateVo.hasMajorUpdate()) log.debug("  - Major: {} → {}", existingEntity.getMajor(), memberUpdateVo.getMajorValue());
            if (memberUpdateVo.hasSkillsUpdate()) log.debug("  - Skills: {} items → {} items",
                    existingEntity.getSkills() != null ? existingEntity.getSkills().length : 0,
                    memberUpdateVo.getSkillsValues() != null ? memberUpdateVo.getSkillsValues().size() : 0);
            if (memberUpdateVo.hasDescriptionUpdate()) log.debug("  - Description updated");

            // VO를 사용하여 Entity 업데이트
            MemberEntity updatedEntity = memberInfrastructureMapper.updateEntity(existingEntity, memberUpdateVo);
            log.debug("✅ MemberEntity updated using VO");

            // ================================================================
            // STEP 3: JPA를 통한 Entity 영속화
            // ================================================================

            log.debug("💾 Persisting updated MemberEntity to database...");
            MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
            log.debug("✅ Entity persisted with ID: {}", savedEntity.getId());

            // ================================================================
            // STEP 4: Entity → VO 변환 (결과 반환용)
            // 저장된 Entity를 MemberUpdatedVo로 변환하여 Domain Layer로 반환
            // ================================================================

            log.debug("🔄 Converting saved MemberEntity to MemberUpdatedVo...");
            MemberUpdatedVo result = memberInfrastructureMapper.toMemberUpdatedVo(savedEntity);
            log.debug("✅ Entity → VO conversion completed");
            log.debug("📤 Returning MemberUpdatedVo with ID: {}", result.id().value());

            log.info("🎉 Infrastructure: Member updated successfully with ID: {}", result.id().value());
            return result;

        } catch (DomainException e) {
            log.warn("❌ Domain exception during member update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during member update for ID: {}", memberId.value(), e);
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_RESOURCE_CONFLICT,
                    "회원 수정 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public MemberVo updateProfile(MemberIdVo memberId, ProfileUpdateVo profileUpdateVo) {
        log.info("Command Infrastructure: Updating member profile with ID: {}", memberId.value());

        try {
            // 기존 Entity 조회
            MemberEntity existingEntity = memberJpaRepository.findById(memberId.value())
                    .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                            "회원을 찾을 수 없습니다: " + memberId.value()));

            // VO를 사용하여 Entity 프로필 업데이트
            MemberEntity updatedEntity = memberInfrastructureMapper.updateProfileEntity(existingEntity, profileUpdateVo);

            // Entity 저장
            MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);

            log.info("Command Infrastructure: Member profile updated successfully with ID: {}",
                    savedEntity.getId());

            // Entity → VO 변환
            return memberInfrastructureMapper.toMemberVo(savedEntity);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating member profile {}: {}", memberId.value(), e.getMessage());
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                    "회원 프로필 수정에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteById(MemberIdVo memberId) {
        log.info("Command Infrastructure: Deleting member with ID: {}", memberId.value());

        try {
            // 존재 확인
            if (!memberJpaRepository.existsById(memberId.value())) {
                throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                        "회원을 찾을 수 없습니다: " + memberId.value());
            }

            // Soft Delete 수행 (JPA Repository의 deleteById는 @SQLDelete에 의해 soft delete됨)
            memberJpaRepository.deleteById(memberId.value());

            log.info("Command Infrastructure: Member deleted successfully: {}", memberId.value());

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting member {}: {}", memberId.value(), e.getMessage());
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                    "회원 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberVo> findById(MemberIdVo memberId) {
        log.debug("Command Infrastructure: Finding member by ID: {}", memberId.value());

        try {
            return memberJpaRepository.findById(memberId.value())
                    .map(memberInfrastructureMapper::toMemberVo);

        } catch (Exception e) {
            log.error("Error finding member by ID {}: {}", memberId.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStudentNumber(StudentNumberVo studentNumber) {
        log.debug("Command Infrastructure: Checking if student number exists: {}",
                studentNumber.value());

        try {
            return memberJpaRepository.existsByStudentNumber(studentNumber.value());

        } catch (Exception e) {
            log.error("Error checking student number existence {}: {}",
                    studentNumber.value(), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(MemberIdVo memberId) {
        log.debug("Command Infrastructure: Checking if member exists by ID: {}", memberId.value());

        try {
            return memberJpaRepository.existsById(memberId.value());

        } catch (Exception e) {
            log.error("Error checking member existence by ID {}: {}", memberId.value(), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public MemberVo activateMember(MemberIdVo memberId) {
        log.info("Command Infrastructure: Activating member with ID: {}", memberId.value());

        try {
            // 삭제된 회원도 포함하여 조회 (일반 findById 사용 - 실제로는 Soft Delete 구현 필요)
            Optional<MemberEntity> deletedMember = memberJpaRepository.findById(memberId.value());

            if (deletedMember.isEmpty()) {
                throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                        "회원을 찾을 수 없습니다: " + memberId.value());
            }

            MemberEntity entity = deletedMember.get();

            // deleted_at을 null로 설정하여 활성화
            MemberEntity activatedEntity = entity.toBuilder()
                    .deletedAt(null)
                    .updatedAt(OffsetDateTime.now())
                    .build();

            // Entity 저장
            MemberEntity savedEntity = memberJpaRepository.save(activatedEntity);

            log.info("Command Infrastructure: Member activated successfully: {}", memberId.value());

            // Entity → VO 변환
            return memberInfrastructureMapper.toMemberVo(savedEntity);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error activating member {}: {}", memberId.value(), e.getMessage());
            throw new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                    "회원 활성화에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updatePenalty(MemberIdVo memberIdVo, PenaltyPointsVo penaltyPointsVo) {
        log.info("Infrastructure: Updating penalty for memberId={}, points={}",
                memberIdVo.value(), penaltyPointsVo.points());

        // member_penalty 테이블에서 조회
        MemberPenaltyEntity penaltyEntity = memberPenaltyJpaRepository.findByMemberId(memberIdVo.value())
                .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                        "패널티 정보를 찾을 수 없습니다: " + memberIdVo.value()));

        // 점수 설정 업데이트 (정책: 전달된 점수로 필드를 초기화)
        penaltyEntity.updatePenaltyPoints(penaltyPointsVo.points());

        memberPenaltyJpaRepository.save(penaltyEntity);

        log.info("✅ Infrastructure: Penalty updated successfully for memberId={}, totalPoints={}",
                memberIdVo.value(), penaltyEntity.getPenaltyPoint());
    }


    @Override
    @Transactional
    public void updateGracePeriod(MemberIdVo memberIdVo, GracePeriodVo gracePeriodVo) {
        try {
            log.info("Infrastructure: Updating grace period for memberId={}, until={}",
                    memberIdVo.value(), gracePeriodVo.value());

            MemberEntity member = memberJpaRepository.findById(memberIdVo.value())
                    .orElseThrow(() -> new DomainException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND,
                            "회원을 찾을 수 없습니다: " + memberIdVo.value()));

            log.info("Infrastructure: Found member - id={}, currentGracePeriod={}", 
                    member.getId(), member.getGracePeriod());

            // MemberEntity에 gracePeriod 업데이트
            MemberEntity updated = member.toBuilder()
                    .gracePeriod(gracePeriodVo.value())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            log.info("Infrastructure: Built updated member - id={}, newGracePeriod={}", 
                    updated.getId(), updated.getGracePeriod());

            MemberEntity saved = memberJpaRepository.save(updated);
            
            log.info("Infrastructure: Saved member - id={}, savedGracePeriod={}", 
                    saved.getId(), saved.getGracePeriod());

            log.info("✅ Infrastructure: Grace period updated successfully for memberId={}", memberIdVo.value());
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to update grace period for memberId={}, error: {}", 
                    memberIdVo.value(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void createPenalty(MemberIdVo memberId) {
        log.info("Infrastructure: Creating penalty record for memberId={}", memberId.value());

        try {
            // 신규 회원의 패널티 레코드 생성 (초기값: 0점)
            MemberPenaltyEntity penaltyEntity = MemberPenaltyEntity.builder()
                    .memberId(memberId.value())
                    .penaltyPoint(0)
                    .penaltiedAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            memberPenaltyJpaRepository.save(penaltyEntity);

            log.info("✅ Infrastructure: Penalty record created successfully for memberId={}", memberId.value());
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to create penalty record for memberId={}, error: {}", 
                    memberId.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.MEMBER_INFRASTRUCTURE_DATABASE_ERROR,
                    "패널티 레코드 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}