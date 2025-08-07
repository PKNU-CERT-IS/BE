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
 * Entity To Domain Mapper
 * 
 * ✅ JPA Entity → Domain Entity 변환 담당
 * ✅ Infrastructure Layer의 JPA Entity → Domain 전용 매퍼
 * ✅ 네이밍 컨벤션: EntityToDomainMapper
 * ✅ Raw Data → Domain Entity 변환 지원 (jOOQ 쿼리 결과용)
 * ✅ 빌더 패턴 사용으로 깔끔한 객체 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityToDomainMapper {

    private final ObjectMapper objectMapper;

    /**
     * MemberEntity → Domain Member 변환 (메인 메서드)
     * 빌더 패턴 사용으로 깔끔한 객체 생성
     */
    public Member toMember(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return Member.builder()
                .id(entity.getId())
                .name(mapStringToNameVo(entity.getName()))
                .studentNumber(mapStringToStudentNumberVo(entity.getStudentNumber()))
                .profileImage(mapStringToProfileImageVo(entity.getProfileImage()))
                .grade(mapStringToGradeVo(entity.getGrade()))
                .role(mapStringToRoleVo(entity.getRole()))
                .skills(mapStringArrayToSkillsVo(entity.getSkills()))
                .major(mapStringToMajorVo(entity.getMajor()))
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Raw Data → Domain Member 변환 (jOOQ 쿼리 결과용)
     * 빌더 패턴 사용으로 깔끔한 객체 생성
     */
    public Member toMember(Long id, String name, String studentNumber, String profileImage,
                          String grade, String role, Object skills, String major, String description,
                          ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return Member.builder()
                .id(id)
                .name(mapStringToNameVo(name))
                .studentNumber(mapStringToStudentNumberVo(studentNumber))
                .profileImage(mapStringToProfileImageVo(profileImage))
                .grade(mapStringToGradeVo(grade))
                .role(mapStringToRoleVo(role))
                .skills(mapObjectToSkillsVo(skills))
                .major(mapStringToMajorVo(major))
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * MemberEntity → Profile Domain 변환
     *
     * @param entity Member Entity
     * @return Profile Domain 객체
     */
    public Profile toProfile(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Profile(
                entity.getId(),
                entity.getName(), // Member의 name을 Profile name으로 사용
                entity.getDescription(),
                entity.getProfileImage()
        );
    }

    /**
     * MemberEntity → Profile Domain 변환 (기존 Profile 정보 포함)
     *
     * @param entity Member Entity
     * @return Profile Domain 객체 (기존 Profile 정보 포함)
     */
    public Profile toProfileWithExistingData(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Profile(
                entity.getId(),
                entity.getId(), // memberId
                entity.getName(),
                entity.getDescription(),
                entity.getProfileImage() != null ? new ProfileImageVo(entity.getProfileImage()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // =================================================================
    // Private Helper Methods
    // =================================================================

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

    /**
     * Object → SkillsVo 변환 (jOOQ 쿼리 결과용)
     */
    private SkillsVo mapObjectToSkillsVo(Object skills) {
        if (skills == null) {
            return new SkillsVo(List.of());
        }

        List<String> skillsList = parseSkillsFromDatabase(skills);
        return new SkillsVo(skillsList);
    }

    /**
     * 데이터베이스에서 조회된 skills 데이터를 파싱
     */
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
     * PostgreSQL 배열 형식 파싱
     */
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

    /**
     * JSON 배열 형식 파싱
     */
    private List<String> parseJsonArray(String jsonArray) {
        try {
            return objectMapper.readValue(jsonArray, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("JSON 배열 파싱 중 오류 발생: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 콤마로 구분된 문자열 파싱
     */
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