package org.certis.studyplatform.member.infrastructure.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @deprecated 이 클래스는 더 이상 사용되지 않습니다.
 * 대신 다음 매퍼들을 사용하세요:
 * - {@link DomainToEntityMapper}: Domain Entity → JPA Entity 변환
 * - {@link EntityToDomainMapper}: JPA Entity → Domain Entity 변환
 * 
 * Clean Architecture 원칙에 따라 단일 책임 원칙을 적용하여 매퍼를 분리했습니다.
 */
@Deprecated(since = "1.0", forRemoval = true)
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberMapper {

    private final ObjectMapper objectMapper;

    // =================================================================
    // Public Methods
    // =================================================================

    /**
     * Domain Member -> MemberEntity 변환 (메인 메서드)
     * 
     * @deprecated {@link DomainToEntityMapper#toMemberEntity(Member)} 사용
     */
    @Deprecated
    public MemberEntity toEntity(Member member) {
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
     * MemberEntity -> Domain Member 변환 (메인 메서드)
     * 
     * @deprecated {@link EntityToDomainMapper#toMember(MemberEntity)} 사용
     */
    @Deprecated
    public Member toDomain(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Member(
                entity.getId(),
                mapStringToNameVo(entity.getName()),
                mapStringToStudentNumberVo(entity.getStudentNumber()),
                mapStringToProfileImageVo(entity.getProfileImage()),
                mapStringToGradeVo(entity.getGrade()),
                mapStringToRoleVo(entity.getRole()),
                mapStringArrayToSkillsVo(entity.getSkills()),
                mapStringToMajorVo(entity.getMajor()),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 데이터베이스에서 조회된 skills 데이터를 파싱
     *
     * @param skillsData 데이터베이스의 skills 컬럼 데이터 (JSON 배열 또는 String 배열)
     * @return 파싱된 기술 스택 리스트
     * 
     * @deprecated {@link EntityToDomainMapper#parseSkillsFromDatabase(Object)} 사용
     */
    @Deprecated
    public List<String> parseSkillsFromDatabase(Object skillsData) {
        if (skillsData == null) {
            return List.of();
        }

        try {
            // String 배열인 경우
            if (skillsData instanceof String[]) {
                return Arrays.asList((String[]) skillsData);
            }

            // JSON 문자열인 경우
            if (skillsData instanceof String) {
                String jsonString = (String) skillsData;
                if (jsonString.trim().isEmpty() || "null".equals(jsonString)) {
                    return List.of();
                }

                // JSON 배열 파싱
                List<String> skills = objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
                return skills != null ? skills : List.of();
            }

            // PostgreSQL 배열인 경우
            if (skillsData instanceof String && ((String) skillsData).startsWith("{")) {
                return parsePostgreSQLArray((String) skillsData);
            }

            // 콤마로 구분된 문자열인 경우
            if (skillsData instanceof String) {
                return parseCommaSeparatedString((String) skillsData);
            }

            log.warn("지원하지 않는 skills 데이터 타입: {}", skillsData.getClass().getSimpleName());
            return List.of();

        } catch (Exception e) {
            log.error("skills 데이터 파싱 중 오류 발생: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 기술 스택을 데이터베이스 저장용 JSON으로 직렬화
     * 
     * @deprecated {@link DomainToEntityMapper#serializeSkillsForDatabase(List)} 사용
     */
    @Deprecated
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