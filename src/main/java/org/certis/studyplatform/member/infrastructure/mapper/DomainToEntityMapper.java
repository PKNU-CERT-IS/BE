package org.certis.studyplatform.member.infrastructure.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Domain To Entity Mapper
 * 
 * ✅ Domain Entity → JPA Entity 변환 담당
 * ✅ Infrastructure Layer의 Domain → JPA Entity 전용 매퍼
 * ✅ 네이밍 컨벤션: DomainToEntityMapper
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainToEntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * Domain Member → MemberEntity 변환 (메인 메서드)
     */
    public MemberEntity toMemberEntity(Member member) {
        if (member == null) {
            return null;
        }

        return MemberEntity.builder()
                .id(member.getId())
                .name(mapNameVoToString(member.getName()))
                .studentNumber(mapStudentNumberVoToString(member.getStudentNumber()))
                .profileImage(mapProfileImageVoToString(member.getProfileImage()))
                .grade(mapGradeVoToString(member.getGrade()))
                .role(mapRoleVoToString(member.getRole()))
                .major(mapMajorVoToString(member.getMajor()))
                .description(member.getDescription())
                .skills(mapSkillsVoToStringArray(member.getSkills()))
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    /**
     * Domain Member → MemberEntity 변환 (ID만)
     */
    public MemberEntity toMemberEntity(Long memberId) {
        return MemberEntity.builder()
                .id(memberId)
                .build();
    }

    /**
     * Profile Domain → MemberEntity 변환 (새 Entity 생성)
     *
     * @param profile Profile Domain 객체
     * @param memberInfo 기본 Member 정보
     * @return Profile 정보가 포함된 새 Member Entity
     */
    public MemberEntity toMemberEntityFromProfile(Profile profile, MemberEntityInfo memberInfo) {
        if (profile == null || memberInfo == null) {
            throw new IllegalArgumentException("Profile and MemberInfo cannot be null");
        }

        return MemberEntity.builder()
                .id(profile.getMemberId())
                .name(memberInfo.name())
                .studentNumber(memberInfo.studentNumber())
                .grade(memberInfo.grade())
                .role(memberInfo.role())
                .major(memberInfo.major())
                .description(memberInfo.description())
                .skills(memberInfo.skills())
                .createdAt(memberInfo.createdAt())
                .updatedAt(profile.getUpdatedAt())
                // Profile 정보
                .description(profile.getDescription())
                .profileImage(profile.getProfileImageValue())
                .createdAt(profile.getCreatedAt() != null ? profile.getCreatedAt() : memberInfo.createdAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Profile Domain 정보로 기존 MemberEntity 업데이트 (Builder 사용)
     *
     * @param existingEntity 기존 Member Entity
     * @param profile Profile Domain 객체
     * @return Profile 정보가 반영된 새로운 Member Entity
     */
    public MemberEntity updateMemberEntityWithProfile(MemberEntity existingEntity, Profile profile) {
        if (existingEntity == null || profile == null) {
            throw new IllegalArgumentException("ExistingEntity and Profile cannot be null");
        }

        return existingEntity.toBuilder()
                .description(profile.getDescription())
                .profileImage(profile.getProfileImageValue())
                .createdAt(profile.getCreatedAt() != null ? profile.getCreatedAt() : existingEntity.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Profile만 업데이트 (Member 정보는 유지)
     *
     * @param existingEntity 기존 Member Entity
     * @param description 새 설명
     * @param profileImage 새 프로필 이미지 URL
     * @return 업데이트된 새 Member Entity
     */
    public MemberEntity updateProfileFields(MemberEntity existingEntity,
                                            String description,
                                            String profileImage) {
        if (existingEntity == null) {
            throw new IllegalArgumentException("ExistingEntity cannot be null");
        }

        ZonedDateTime now = ZonedDateTime.now();

        return existingEntity.toBuilder()
                .description(description)
                .profileImage(profileImage)
                .updatedAt(now)
                .build();
    }

    /**
     * MemberEntity에서 Profile 관련 필드 제거 (Builder 사용)
     *
     * @param existingEntity 기존 Member Entity
     * @return Profile 정보가 제거된 새 Member Entity
     */
    public MemberEntity clearProfileFromMemberEntity(MemberEntity existingEntity) {
        if (existingEntity == null) {
            return null;
        }

        return existingEntity.toBuilder()
                .description(null)
                .profileImage(null)
                .build();
    }

    /**
     * Profile Domain으로부터 최소한의 MemberEntity 생성
     *
     * @param profile Profile Domain 객체
     * @return Profile 정보만 있는 Member Entity (Member 필드는 최소한으로)
     */
    public MemberEntity createMinimalMemberEntityForProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }

        return MemberEntity.builder()
                .id(profile.getMemberId())
                .name(profile.getName())
                // Profile 정보
                .description(profile.getDescription())
                .profileImage(profile.getProfileImageValue())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Entity가 Profile 정보를 가지고 있는지 확인
     *
     * @param entity Member Entity
     * @return Profile 정보 존재 여부
     */
    public boolean hasProfileInformation(MemberEntity entity) {
        if (entity == null) {
            return false;
        }

        return entity.getDescription() != null ||
                entity.getProfileImage() != null;
    }

    // =================================================================
    // Helper Record for Member Information
    // =================================================================

    /**
     * Member 기본 정보를 담는 Record
     * Profile 생성 시 필요한 Member 정보 전달용
     */
    public record MemberEntityInfo(
            String name,
            String studentNumber,
            String grade,
            String role,
            String major,
            String description,
            String[] skills,
            ZonedDateTime createdAt
    ) {

        /**
         * 기존 Member Entity로부터 정보 추출
         */
        public static MemberEntityInfo from(MemberEntity entity) {
            if (entity == null) {
                return null;
            }

            return new MemberEntityInfo(
                    entity.getName(),
                    entity.getStudentNumber(),
                    entity.getGrade(),
                    entity.getRole(),
                    entity.getMajor(),
                    entity.getDescription(),
                    entity.getSkills(),
                    entity.getCreatedAt()
            );
        }
    }

    // =================================================================
    // Private Helper Methods
    // =================================================================

    private String mapNameVoToString(NameVo nameVo) {
        return nameVo != null ? nameVo.value() : null;
    }

    private String mapStudentNumberVoToString(StudentNumberVo studentNumberVo) {
        return studentNumberVo != null ? studentNumberVo.value() : null;
    }

    private String mapProfileImageVoToString(ProfileImageVo profileImageVo) {
        return profileImageVo != null ? profileImageVo.value() : null;
    }

    private String mapGradeVoToString(GradeVo gradeVo) {
        return gradeVo != null ? gradeVo.value() : null;
    }

    private String mapRoleVoToString(RoleVo roleVo) {
        return roleVo != null ? roleVo.value() : null;
    }

    private String mapMajorVoToString(MajorVo majorVo) {
        return majorVo != null ? majorVo.value() : null;
    }

    private String[] mapSkillsVoToStringArray(SkillsVo skillsVo) {
        if (skillsVo == null || skillsVo.values() == null) {
            return new String[0];
        }

        List<String> skillsList = skillsVo.values();
        return skillsList.toArray(new String[0]);
    }

    /**
     * 기술 스택을 데이터베이스 저장용 JSON으로 직렬화
     */
    public String serializeSkillsForDatabase(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception e) {
            log.error("기술 스택 직렬화 중 오류 발생: {}", e.getMessage(), e);
            return "[]";
        }
    }
} 