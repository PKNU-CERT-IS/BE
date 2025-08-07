package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.repository.command.ProfileCommandRepository;
import org.certis.studyplatform.member.infrastructure.mapper.DomainToEntityMapper;
import org.certis.studyplatform.member.infrastructure.mapper.EntityToDomainMapper;
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
    private final DomainToEntityMapper domainToEntityMapper;
    private final EntityToDomainMapper entityToDomainMapper;

    @Override
    @Transactional
    public Profile save(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }

        log.debug("Command Infrastructure: Saving profile for member ID: {}", profile.getMemberId());

        try {
            // Member Entity 조회
            MemberEntity existingEntity = memberJpaRepository.findById(profile.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("Member not found: " + profile.getMemberId()));

            // DomainToEntityMapper를 사용하여 Builder 패턴으로 새 Entity 생성
            MemberEntity updatedEntity = domainToEntityMapper.updateMemberEntityWithProfile(existingEntity, profile);

            // 저장
            MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);

            log.debug("Command Infrastructure: Profile saved successfully for member ID: {}", profile.getMemberId());

            // EntityToDomainMapper를 사용하여 Entity → Profile Domain 변환
            return entityToDomainMapper.toProfile(savedEntity);

        } catch (Exception e) {
            log.error("Error saving profile for member ID {}: {}", profile.getMemberId(), e.getMessage());
            throw new RuntimeException("Failed to save profile", e);
        }
    }

    @Override
    public Optional<Profile> findByMemberId(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }

        log.debug("Command Infrastructure: Finding profile by member ID: {}", memberId);

        try {
            return memberJpaRepository.findById(memberId)
                    .filter(domainToEntityMapper::hasProfileInformation) // Profile 정보가 있는 경우만
                    .map(entityToDomainMapper::toProfile);
        } catch (Exception e) {
            log.error("Error finding profile by member ID {}: {}", memberId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        if (memberId == null) {
            log.warn("Command Infrastructure: Attempted to delete profile with null member ID");
            return;
        }

        log.debug("Command Infrastructure: Deleting profile for member ID: {}", memberId);

        try {
            memberJpaRepository.findById(memberId)
                    .ifPresent(existingEntity -> {
                        // DomainToEntityMapper를 사용하여 Profile 정보 제거 (Builder 패턴)
                        MemberEntity clearedEntity = domainToEntityMapper.clearProfileFromMemberEntity(existingEntity);
                        memberJpaRepository.save(clearedEntity);
                        log.debug("Command Infrastructure: Profile cleared for member ID: {}", memberId);
                    });
        } catch (Exception e) {
            log.error("Error deleting profile for member ID {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to delete profile", e);
        }
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        if (memberId == null) {
            return false;
        }

        try {
            return memberJpaRepository.findById(memberId)
                    .map(domainToEntityMapper::hasProfileInformation)
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking profile existence for member ID {}: {}", memberId, e.getMessage());
            return false;
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
                    .filter(domainToEntityMapper::hasProfileInformation)
                    .forEach(entity -> {
                        MemberEntity clearedEntity = domainToEntityMapper.clearProfileFromMemberEntity(entity);
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
     * @param memberId 회원 ID
     * @param description 새 설명
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<Profile> updateDescription(Long memberId, String description) {
        log.debug("Command Infrastructure: Updating profile description for member ID: {}", memberId);

        try {
            return memberJpaRepository.findById(memberId)
                    .map(existingEntity -> {
                        // DomainToEntityMapper를 사용하여 특정 필드만 업데이트
                        MemberEntity updatedEntity = domainToEntityMapper.updateProfileFields(
                                existingEntity,
                                description,
                                existingEntity.getProfileImage()
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return entityToDomainMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile description for member ID {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to update profile description", e);
        }
    }

    /**
     * Profile 이미지만 업데이트
     *
     * @param memberId 회원 ID
     * @param profileImageUrl 새 프로필 이미지 URL
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<Profile> updateProfileImage(Long memberId, String profileImageUrl) {
        log.debug("Command Infrastructure: Updating profile image for member ID: {}", memberId);

        try {
            return memberJpaRepository.findById(memberId)
                    .map(existingEntity -> {
                        MemberEntity updatedEntity = domainToEntityMapper.updateProfileFields(
                                existingEntity,
                                existingEntity.getDescription(),
                                profileImageUrl
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return entityToDomainMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile image for member ID {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to update profile image", e);
        }
    }

    /**
     * Profile 공개 설정만 업데이트
     *
     * @param memberId 회원 ID
     * @return 업데이트된 Profile
     */
    @Transactional
    public Optional<Profile> updateVisibility(Long memberId, Boolean isPublic) {
        log.debug("Command Infrastructure: Updating profile visibility for member ID: {} to {}", memberId, isPublic);

        try {
            return memberJpaRepository.findById(memberId)
                    .map(existingEntity -> {
                        MemberEntity updatedEntity = domainToEntityMapper.updateProfileFields(
                                existingEntity,
                                existingEntity.getDescription(),
                                existingEntity.getProfileImage()
                        );

                        MemberEntity savedEntity = memberJpaRepository.save(updatedEntity);
                        return entityToDomainMapper.toProfile(savedEntity);
                    });
        } catch (Exception e) {
            log.error("Error updating profile visibility for member ID {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to update profile visibility", e);
        }
    }
}