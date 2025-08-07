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
import java.util.Arrays;
import java.util.List;

/**
 * Member Infrastructure Mapper (Facade)
 * 
 * ✅ Infrastructure Layer의 통합 매퍼 (Facade Pattern)
 * ✅ DomainToEntityMapper, EntityToDomainMapper를 통합 관리
 * ✅ 네이밍 컨벤션: MemberMapper
 * ✅ 하위 호환성 유지 (기존 메서드들 유지)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberMapper {

    private final DomainToEntityMapper domainToEntityMapper;
    private final EntityToDomainMapper entityToDomainMapper;
    private final ObjectMapper objectMapper;

    // =================================================================
    // Domain → Entity 변환 (DomainToEntityMapper 위임)
    // =================================================================

    /**
     * Domain Member → MemberEntity 변환 (메인 메서드)
     */
    public MemberEntity toEntity(Member member) {
        return domainToEntityMapper.toMemberEntity(member);
    }

    /**
     * Domain Member → MemberEntity 변환 (ID만)
     */
    public MemberEntity toEntity(Long memberId) {
        return domainToEntityMapper.toMemberEntity(memberId);
    }

    /**
     * Profile Domain → MemberEntity 변환 (새 Entity 생성)
     */
    public MemberEntity toEntityFromProfile(Profile profile, DomainToEntityMapper.MemberEntityInfo memberInfo) {
        return domainToEntityMapper.toMemberEntityFromProfile(profile, memberInfo);
    }

    /**
     * Profile Domain 정보로 기존 MemberEntity 업데이트
     */
    public MemberEntity updateEntityWithProfile(MemberEntity existingEntity, Profile profile) {
        return domainToEntityMapper.updateMemberEntityWithProfile(existingEntity, profile);
    }

    /**
     * Profile만 업데이트 (Member 정보는 유지)
     */
    public MemberEntity updateProfileFields(MemberEntity existingEntity, String description, String profileImage) {
        return domainToEntityMapper.updateProfileFields(existingEntity, description, profileImage);
    }

    /**
     * MemberEntity에서 Profile 관련 필드 제거
     */
    public MemberEntity clearProfileFromEntity(MemberEntity existingEntity) {
        return domainToEntityMapper.clearProfileFromMemberEntity(existingEntity);
    }

    /**
     * Profile Domain으로부터 최소한의 MemberEntity 생성
     */
    public MemberEntity createMinimalEntityForProfile(Profile profile) {
        return domainToEntityMapper.createMinimalMemberEntityForProfile(profile);
    }

    /**
     * Entity가 Profile 정보를 가지고 있는지 확인
     */
    public boolean hasProfileInformation(MemberEntity entity) {
        return domainToEntityMapper.hasProfileInformation(entity);
    }

    // =================================================================
    // Entity → Domain 변환 (EntityToDomainMapper 위임)
    // =================================================================

    /**
     * MemberEntity → Domain Member 변환 (메인 메서드)
     */
    public Member toDomain(MemberEntity entity) {
        return entityToDomainMapper.toMember(entity);
    }

    /**
     * Raw Data → Domain Member 변환 (jOOQ 쿼리 결과용)
     */
    public Member toDomain(Long id, String name, String studentNumber, String profileImage,
                          String grade, String role, Object skills, String major, String description,
                          ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return entityToDomainMapper.toMember(id, name, studentNumber, profileImage, grade, role, 
                                           skills, major, description, createdAt, updatedAt);
    }

    /**
     * MemberEntity → Profile Domain 변환
     */
    public Profile toProfile(MemberEntity entity) {
        return entityToDomainMapper.toProfile(entity);
    }

    /**
     * MemberEntity → Profile Domain 변환 (기존 Profile 정보 포함)
     */
    public Profile toProfileWithExistingData(MemberEntity entity) {
        return entityToDomainMapper.toProfileWithExistingData(entity);
    }

    // =================================================================
    // Helper Methods (EntityToDomainMapper 위임)
    // =================================================================

    /**
     * 데이터베이스에서 조회된 skills 데이터를 파싱
     */
    public List<String> parseSkillsFromDatabase(Object skillsData) {
        return entityToDomainMapper.parseSkillsFromDatabase(skillsData);
    }

    // =================================================================
    // Helper Methods (DomainToEntityMapper 위임)
    // =================================================================

    /**
     * 기술 스택을 데이터베이스 저장용 JSON으로 직렬화
     */
    public String serializeSkillsForDatabase(List<String> skills) {
        return domainToEntityMapper.serializeSkillsForDatabase(skills);
    }

    // =================================================================
    // Helper Record (DomainToEntityMapper 위임)
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
    // 하위 호환성을 위한 기존 메서드들 (Deprecated)
    // =================================================================

    /**
     * @deprecated {@link #toEntity(Member)} 사용
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public MemberEntity toMemberEntity(Member member) {
        return toEntity(member);
    }

    /**
     * @deprecated {@link #toDomain(MemberEntity)} 사용
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public Member toMember(MemberEntity entity) {
        return toDomain(entity);
    }

    // =================================================================
    // Private Helper Methods (기존 구현 유지 - 하위 호환성)
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

    private NameVo mapStringToNameVo(String name) {
        return name != null ? NameVo.of(name) : null;
    }

    private StudentNumberVo mapStringToStudentNumberVo(String studentNumber) {
        return studentNumber != null ? new StudentNumberVo(studentNumber) : null;
    }

    private ProfileImageVo mapStringToProfileImageVo(String profileImage) {
        return profileImage != null ? new ProfileImageVo(profileImage) : null;
    }

    private GradeVo mapStringToGradeVo(String grade) {
        return grade != null ? GradeVo.of(grade) : null;
    }

    private RoleVo mapStringToRoleVo(String role) {
        return role != null ? RoleVo.of(role) : null;
    }

    private MajorVo mapStringToMajorVo(String major) {
        return major != null ? MajorVo.of(major) : null;
    }

    private SkillsVo mapStringArrayToSkillsVo(String[] skills) {
        if (skills == null || skills.length == 0) {
            return new SkillsVo(List.of());
        }

        List<String> skillsList = Arrays.asList(skills);
        return new SkillsVo(skillsList);
    }

    private List<String> parsePostgreSQLArray(String pgArray) {
        if (pgArray == null || pgArray.trim().isEmpty()) {
            return List.of();
        }

        // PostgreSQL 배열 형식: {item1,item2,item3}
        String content = pgArray.substring(1, pgArray.length() - 1);
        if (content.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> parseJsonArray(String jsonArray) {
        try {
            return objectMapper.readValue(jsonArray, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("JSON 배열 파싱 중 오류 발생: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<String> parseCommaSeparatedString(String csvString) {
        if (csvString == null || csvString.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(csvString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}