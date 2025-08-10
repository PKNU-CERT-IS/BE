package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.repository.command.ProfileCommandRepository;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Profile Command Repository 구현체 (JPA + Builder 패턴)
 *
 * ProfileDomainConverter와 Builder 패턴을 활용한 Profile 관리
 * Member Entity를 활용하여 Profile Domain 영속화
 *
 * 특징:
 * - ProfileDomainConverter로 변환 로직 분리
 * - Builder 패턴으로 불변성과 안전성 보장
 * - 명확한 책임 분리로 유지보수성 향상
 *
 * 책임:
 * - Profile Domain의 영속화 관리
 * - Member Entity와 Profile Domain 간 중재
 * - 트랜잭션 관리 및 예외 처리
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProfileCommandRepositoryImpl implements ProfileCommandRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberInfrastructureMapper memberInfrastructureMapper;

    @Override
    @Transactional
    public ProfileVo save(ProfileVo profileVo) {
        if (profileVo == null) {
            throw new InfrastructureException(ExceptionStatus.PROFILE_INFRASTRUCTURE_NOT_FOUND);
        }

        log.debug("Command Infrastructure: Saving profile for member ID: {}", profileVo.memberId());

        try {
            // Member Entity 조회
            MemberEntity existingEntity = memberJpaRepository.findById(profileVo.memberId())
                    .orElseThrow(() -> new InfrastructureException(ExceptionStatus.PROFILE_INFRASTRUCTURE_NOT_FOUND,"Member not found: " + profileVo.memberId()));

            // DomainToEntityMapper를 사용하여 Builder 패턴으로 새 Entity 생성
            MemberEntity updatedEntity = memberInfrastructureMapper.updateMemberEntityWithProfile(existingEntity, profileVo);

            // 저장
            MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);

            log.debug("Command Infrastructure: Profile saved successfully for member ID: {}", profileVo.memberId());

            // EntityToDomainMapper를 사용하여 Entity → Profile Domain 변환
            return memberInfrastructureMapper.toProfile(savedEntity);

        } catch (Exception e) {
            log.error("Error saving profile for member ID {}: {}", profileVo.memberId(), e.getMessage());
            throw new InfrastructureException(ExceptionStatus.PROFILE_INFRASTRUCTURE_DATABASE_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteByMemberId(MemberIdVo memberIdVo) {
        log.debug("Command Infrastructure: Deleting profile for member ID: {}", memberIdVo.toLong());

        try {
            memberJpaRepository.findById(memberIdVo.toLong())
                    .ifPresent(existingEntity -> {
                        // DomainToEntityMapper를 사용하여 Profile 정보 제거 (Builder 패턴)
                        MemberEntity clearedEntity = memberInfrastructureMapper.clearProfileFromMemberEntity(existingEntity);
                        memberJpaRepository.save(clearedEntity);
                        log.debug("Command Infrastructure: Profile cleared for member ID: {}", memberIdVo.toLong());
                    });
        } catch (Exception e) {
            log.error("Error deleting profile for member ID {}: {}", memberIdVo.toLong(), e.getMessage());
            throw new RuntimeException("Failed to delete profile", e);
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        log.warn("Command Infrastructure: Clearing all profile information (TEST ONLY)");

        try {
            // 모든 Member Entity에서 Profile 정보만 제거
            memberJpaRepository.findAll()
                    .stream()
                    .filter(memberInfrastructureMapper::hasProfileInformation)
                    .forEach(entity -> {
                        MemberEntity clearedEntity = memberInfrastructureMapper.clearProfileFromMemberEntity(entity);
                        memberJpaRepository.save(clearedEntity);
                    });

            log.warn("Command Infrastructure: All profile information cleared");
        } catch (Exception e) {
            log.error("Error clearing all profiles: {}", e.getMessage());
            throw new RuntimeException("Failed to clear all profiles", e);
        }
    }

    // =================================================================
    // Additional Profile-specific Methods (Builder 패턴 활용)
    // =================================================================

    /**
     * Profile 설명만 업데이트
     *
     * @param memberIdVo 회원 ID
     * @param description 새 설명
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<ProfileVo> updateDescription(MemberIdVo memberIdVo, String description) {
        log.debug("Command Infrastructure: Updating profile description for member ID: {}", memberIdVo.toLong());

        try {
            return memberJpaRepository.findById(memberIdVo.toLong())
                    .map(existingEntity -> {
                        // DomainToEntityMapper를 사용하여 특정 필드만 업데이트
                        MemberEntity updatedEntity = memberInfrastructureMapper.updateProfileFields(
                                existingEntity,
                                description,
                                existingEntity.getProfileImage()
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return memberInfrastructureMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile description for member ID {}: {}", memberIdVo.toLong(), e.getMessage());
            throw new RuntimeException("Failed to update profile description", e);
        }
    }

    /**
     * Profile 이미지만 업데이트
     *
     * @param memberIdVo 회원 ID
     * @param profileImageUrl 새 프로필 이미지 URL
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<ProfileVo> updateProfileImage(MemberIdVo memberIdVo, String profileImageUrl) {
        log.debug("Command Infrastructure: Updating profile image for member ID: {}", memberIdVo.toLong());

        try {
            return memberJpaRepository.findById(memberIdVo.toLong())
                    .map(existingEntity -> {
                        MemberEntity updatedEntity = memberInfrastructureMapper.updateProfileFields(
                                existingEntity,
                                existingEntity.getDescription(),
                                profileImageUrl
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return memberInfrastructureMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile image for member ID {}: {}", memberIdVo.toLong(), e.getMessage());
            throw new RuntimeException("Failed to update profile image", e);
        }
    }

    /**
     * Profile 공개 설정만 업데이트
     *
     * @param memberIdVo 회원 ID
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<ProfileVo> updateVisibility(MemberIdVo memberIdVo, Boolean isPublic) {
        log.debug("Command Infrastructure: Updating profile visibility for member ID: {} to {}", memberIdVo.toLong(), isPublic);

        try {
            return memberJpaRepository.findById(memberIdVo.toLong())
                    .map(existingEntity -> {
                        MemberEntity updatedEntity = memberInfrastructureMapper.updateProfileFields(
                                existingEntity,
                                existingEntity.getDescription(),
                                existingEntity.getProfileImage()
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return memberInfrastructureMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile visibility for member ID {}: {}", memberIdVo.toLong(), e.getMessage());
            throw new RuntimeException("Failed to update profile visibility", e);
        }
    }
}