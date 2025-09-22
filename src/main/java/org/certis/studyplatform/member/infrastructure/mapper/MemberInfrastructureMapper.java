package org.certis.studyplatform.member.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.infrastructure.persistence.MemberQueryRepositoryImpl;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Member Infrastructure Mapper (Facade)
 *
 * ✅ Infrastructure Layer의 통합 매퍼 (Facade Pattern)
 * ✅ MemberInfrastructureVoMapper, MemberInfrastructureEntityMapper를 통합 관리
 * ✅ Repository 구현체에서 MemberEntity ↔ VO 변환 시 사용
 *
 * 책임:
 * - MemberEntity ↔ Domain VO 변환 통합 관리
 * - Infrastructure Layer의 데이터 변환 중앙 집중화
 * - Repository 구현체에서 사용하는 모든 변환 로직 제공
 *
 * 특징:
 * - Entity → VO 변환은 VoMapper에 위임
 * - VO → Entity 변환은 EntityMapper에 위임
 * - Repository 구현체에서는 이 Facade만 의존
 * - Infrastructure Layer의 변환 로직 단일 진입점
 */
@Component
@RequiredArgsConstructor
public class MemberInfrastructureMapper {

    private final MemberInfrastructureVoMapper memberInfrastructureVoMapper;
    private final MemberInfrastructureEntityMapper memberInfrastructureEntityMapper;

    // =================================================================
    // MemberEntity → Domain VO 변환 (VoMapper 위임)
    // Repository 조회 시 Entity를 VO로 변환하여 Domain Layer로 반환
    // =================================================================

    /**
     * MemberEntity → MemberVo 변환
     */
    public MemberVo toMemberVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMemberVo(entity);
    }

    /**
     * MemberEntity → MemberCreatedVo 변환
     */
    public MemberCreatedVo toMemberCreatedVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMemberCreatedVo(entity);
    }

    /**
     * MemberEntity → MemberUpdatedVo 변환
     */
    public MemberUpdatedVo toMemberUpdatedVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMemberUpdatedVo(entity);
    }

    /**
     * MemberEntity → MemberSummaryVo 변환
     */
    public MemberSummaryVo toMemberSummaryVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMemberSummaryVo(entity);
    }

    /**
     * MemberEntity → 개별 VO 변환들
     */
    public MemberIdVo toMemberIdVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMemberIdVo(entity);
    }

    public NameVo toNameVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toNameVo(entity);
    }

    public StudentNumberVo toStudentNumberVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toStudentNumberVo(entity);
    }

    public GradeVo toGradeVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toGradeVo(entity);
    }

    public RoleVo toRoleVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toRoleVo(entity);
    }

    public MajorVo toMajorVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toMajorVo(entity);
    }

    public SkillsVo toSkillsVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toSkillsVo(entity);
    }

    public ProfileImageVo toProfileImageVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toProfileImageVo(entity);
    }

    public BirthdayVo toBirthdayVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toBirthdayVo(entity);
    }

    public GenderVo toGenderVo(MemberEntity entity) {
        return memberInfrastructureVoMapper.toGenderVo(entity);
    }

    // =================================================================
    // Domain VO → MemberEntity 변환 (EntityMapper 위임)
    // Repository 저장 시 VO를 Entity로 변환하여 영속성 계층에 저장
    // =================================================================

    /**
     * MemberCreationVo → MemberEntity 변환 (새로 생성)
     */
    public MemberEntity toEntity(MemberCreationVo creationVo) {
        return memberInfrastructureEntityMapper.toEntity(creationVo);
    }

    /**
     * MemberCreationVo → MemberEntity 변환 (생년월일, 성별 포함)
     */
    public MemberEntity toEntity(MemberCreationVo creationVo, BirthdayVo birthday, GenderVo gender) {
        return memberInfrastructureEntityMapper.toEntity(creationVo, birthday, gender);
    }

    /**
     * MemberUpdateVo → MemberEntity 부분 업데이트
     */
    public MemberEntity updateEntity(MemberEntity existingEntity, MemberUpdateVo updateVo) {
        return memberInfrastructureEntityMapper.updateEntity(existingEntity, updateVo);
    }

    /**
     * ProfileUpdateVo → MemberEntity 프로필 업데이트
     */
    public MemberEntity updateProfileEntity(MemberEntity existingEntity, ProfileUpdateVo profileUpdateVo) {
        return memberInfrastructureEntityMapper.updateProfileEntity(existingEntity, profileUpdateVo);
    }

    /**
     * 개별 VO들로부터 MemberEntity 생성 (재구성용)
     */
    public MemberEntity toEntity(Long id, NameVo name, StudentNumberVo studentNumber,
                                 ProfileImageVo profileImage, GradeVo grade, RoleVo role,
                                 SkillsVo skills, MajorVo major, String description,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return memberInfrastructureEntityMapper.toEntity(id, name, studentNumber, profileImage,
                grade, role, skills, major, description, createdAt, updatedAt);
    }

    /**
     * 완전한 정보로 MemberEntity 생성 (모든 필드 포함)
     */
    public MemberEntity toEntity(Long id, NameVo name, StudentNumberVo studentNumber,
                                 ProfileImageVo profileImage, GradeVo grade, RoleVo role,
                                 SkillsVo skills, MajorVo major, String description,
                                 BirthdayVo birthday, GenderVo gender,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return memberInfrastructureEntityMapper.toEntity(id, name, studentNumber, profileImage,
                grade, role, skills, major, description, birthday, gender, createdAt, updatedAt);
    }

    // =================================================================
    // 검색/필터링 조건 변환 (EntityMapper 위임)
    // =================================================================


    /**
     * MemberFilterVo → JPA 필터 조건 변환
     */
    public MemberInfrastructureEntityMapper.FilterConditionJpa toFilterCondition(MemberFilterVo filterVo) {
        return memberInfrastructureEntityMapper.toFilterCondition(filterVo);
    }

    // =================================================================
    // 추가 유틸리티 메서드들 (VoMapper 위임)
    // =================================================================

    /**
     * 기술 스택 개수 조회
     */
    public int getSkillsCount(MemberEntity entity) {
        return memberInfrastructureVoMapper.getSkillsCount(entity);
    }

    /**
     * 활성 상태 확인 (삭제되지 않은 상태)
     */
    public boolean isActive(MemberEntity entity) {
        return memberInfrastructureVoMapper.isActive(entity);
    }

    // =================================================================
    // Profile 관련 메서드들 (Profile Domain 객체 변환)
    // ProfileCommandRepositoryImpl, ProfileQueryRepositoryImpl에서 사용
    // =================================================================

    /**
     * MemberEntity → ProfileVo 변환
     * Profile Repository에서 Entity를 ProfileVo로 변환할 때 사용
     */
    public ProfileVo toProfile(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> skills = entity.getSkills() != null ?
                Arrays.asList(entity.getSkills()) : List.of();

        MemberGrade memberGrade = entity.getGrade();
        MemberRole memberRole = entity.getRole() != null ? entity.getRole() : MemberRole.NONE; // Safe conversion

        return new ProfileVo(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getProfileImage(),
                List.of(), // todaySchedules (ProfileQueryRepositoryImpl에서 별도 조회하여 설정)
                0,         // penaltyCount (TODO: 실제 벌점 데이터 조회)
                null,      // gracePeriod (도메인 서비스에서 계산됨)
                memberRole,
                memberGrade,
                skills,
                entity.getCreatedAt(),
                // Enhanced profile fields
                entity.getMajor(),
                entity.getBirthday(),
                null, // phoneNumber (ProfileQueryRepositoryImpl에서 contact 정보 조회하여 설정)
                entity.getStudentNumber(),
                null, // email (ProfileQueryRepositoryImpl에서 contact 정보 조회하여 설정)
                null, // githubUrl (ProfileQueryRepositoryImpl에서 contact 정보 조회하여 설정)
                null  // linkedUrl (ProfileQueryRepositoryImpl에서 contact 정보 조회하여 설정)
        );
    }

    /**
     * ProfileVo를 사용하여 MemberEntity 업데이트
     * ProfileCommandRepositoryImpl에서 Profile 정보로 Entity를 업데이트할 때 사용
     */
    public MemberEntity updateMemberEntityWithProfile(MemberEntity existingEntity,
                                                      ProfileVo profileVo) {
        if (existingEntity == null || profileVo == null) {
            throw new IllegalArgumentException("Entity and ProfileVo cannot be null");
        }

        return existingEntity.toBuilder()
                .name(profileVo.name())
                .description(profileVo.description())
                .profileImage(profileVo.profileImage())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * MemberEntity에서 Profile 정보 제거
     * ProfileCommandRepositoryImpl에서 Profile 삭제 시 사용
     */
    public MemberEntity clearProfileFromMemberEntity(MemberEntity existingEntity) {
        if (existingEntity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        return existingEntity.toBuilder()
                .description(null)
                .profileImage(null)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * MemberEntity가 Profile 정보를 가지고 있는지 확인
     * ProfileCommandRepositoryImpl, ProfileQueryRepositoryImpl에서 사용
     */
    public boolean hasProfileInformation(MemberEntity entity) {
        if (entity == null) {
            return false;
        }

        // Profile 정보가 있다고 판단하는 기준:
        // 1. 이름이 있고 (필수)
        // 2. 설명이나 프로필 이미지 중 하나라도 있는 경우
        return entity.getName() != null && !entity.getName().trim().isEmpty() &&
               (entity.getDescription() != null || entity.getProfileImage() != null);
    }

    /**
     * MemberEntity의 특정 Profile 필드들만 업데이트
     * ProfileCommandRepositoryImpl의 개별 필드 업데이트 메서드에서 사용
     */
    public MemberEntity updateProfileFields(MemberEntity existingEntity,
                                           String description,
                                           String profileImageUrl) {
        if (existingEntity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        return existingEntity.toBuilder()
                .description(description)
                .profileImage(profileImageUrl)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * MemberWithContactEntity → MemberWithContactVo 변환
     */
    public MemberWithContactVo toMemberWithContactVo(MemberQueryRepositoryImpl.MemberWithContactEntity entity) {
        MemberEntity member = entity.getMember();
        MemberContactEntity contact = entity.getContact();

        // skills 배열을 List<String>으로 변환
        List<String> skills = member.getSkills() != null ?
                Arrays.asList(member.getSkills()) :
                Collections.emptyList();

        return MemberWithContactVo.of(
                member.getId(),
                member.getName(),
                member.getProfileImage(),
                member.getGrade(),
                member.getRole(),
                skills,
                member.getMajor(),
                member.getDescription(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                // 연락처 정보 (nullable 처리)
                contact != null ? contact.getEmail() : null,
                contact != null ? contact.getGithubUrl() : null,
                contact != null ? contact.getLinkedinUrl() : null
        );
    }

    // =================================================================
    // Profile 업데이트 시 Contact와 Auth 정보 처리
    // =================================================================

    /**
     * ProfileVo의 연락처 정보로 MemberContactEntity 생성/업데이트
     */
    public MemberContactEntity createOrUpdateContactEntity(Long memberId, ProfileVo profileVo) {
        if (memberId == null || profileVo == null) {
            throw new IllegalArgumentException("MemberId and ProfileVo cannot be null");
        }

        return MemberContactEntity.builder()
                .memberId(memberId)
                .email(profileVo.email() != null ? profileVo.email() : "")
                .phoneNumber(profileVo.phoneNumber() != null ? profileVo.phoneNumber() : "")
                .githubUrl(profileVo.githubUrl())
                .linkedinUrl(profileVo.linkedUrl())
                .build();
    }

    /**
     * ProfileVo의 연락처 정보로 기존 MemberContactEntity 업데이트
     */
    public MemberContactEntity updateContactEntity(MemberContactEntity existingEntity, ProfileVo profileVo) {
        if (existingEntity == null || profileVo == null) {
            throw new IllegalArgumentException("ExistingEntity and ProfileVo cannot be null");
        }

        return existingEntity.toBuilder()
                .email(profileVo.email() != null ? profileVo.email() : existingEntity.getEmail())
                .phoneNumber(profileVo.phoneNumber() != null ? profileVo.phoneNumber() : existingEntity.getPhoneNumber())
                .githubUrl(profileVo.githubUrl() != null ? profileVo.githubUrl() : existingEntity.getGithubUrl())
                .linkedinUrl(profileVo.linkedUrl() != null ? profileVo.linkedUrl() : existingEntity.getLinkedinUrl())
                .build();
    }

    /**
     * ProfileVo의 기본 정보로 MemberEntity 업데이트 (Contact 정보 제외)
     */
    public MemberEntity updateMemberEntityWithProfileInfo(MemberEntity existingEntity, ProfileVo profileVo) {
        if (existingEntity == null || profileVo == null) {
            throw new IllegalArgumentException("ExistingEntity and ProfileVo cannot be null");
        }

        return existingEntity.toBuilder()
                .name(profileVo.name())
                .description(profileVo.description())
                .profileImage(profileVo.profileImage())
                .major(profileVo.major() != null ? profileVo.major() : existingEntity.getMajor())
                .birthday(profileVo.birthday() != null ? profileVo.birthday() : existingEntity.getBirthday())
                .studentNumber(profileVo.studentNumber() != null ? profileVo.studentNumber() : existingEntity.getStudentNumber())
                .skills(profileVo.skills() != null ? profileVo.skills().toArray(new String[0]) : existingEntity.getSkills())
                .grade(profileVo.memberGrade() != null ? profileVo.memberGrade() : existingEntity.getGrade())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}