package org.certis.studyplatform.member.infrastructure.mapper;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * Member Infrastructure Entity Mapper
 *
 * ✅ Infrastructure Layer의 Domain VO → MemberEntity 변환 전용 매퍼
 * ✅ Repository 구현체에서 저장할 Entity를 생성할 때 사용
 * ✅ Domain VO → MemberEntity 변환 담당
 * ✅ 빌더 패턴 사용으로 불변성 보장
 *
 * 책임:
 * - Domain VO → MemberEntity 변환 (Repository 저장 시)
 * - Domain Layer의 VO를 Database가 이해하는 Entity로 변환
 * - Domain 계층의 데이터를 Infrastructure 계층으로 전달
 *
 * 사용처:
 * - MemberRepositoryImpl (JPA Repository 구현체)
 * - MemberQueryRepositoryImpl
 * - Infrastructure Layer의 VO → Entity 변환 로직
 */
@Component
public class MemberInfrastructureEntityMapper {

    // =================================================================
    // Domain VO → MemberEntity 변환 (Repository 저장 시 사용)
    // =================================================================

    /**
     * MemberCreationVo → MemberEntity 변환 (새로 생성) - Enhanced VO-Centric
     * Repository에서 신규 Member 생성 시 사용
     *
     * 변환 과정:
     * 1. Domain VO들에서 primitive 값 추출
     * 2. Entity Builder Pattern으로 JPA Entity 생성
     * 3. PostgreSQL Array 타입 등 DB 특화 변환 수행
     */
    public MemberEntity toEntity(MemberCreationVo creationVo) {
        return MemberEntity.builder()
                // 필수 필드 변환 (VO → primitive)
                .name(extractNameValue(creationVo.name()))
                .studentNumber(extractStudentNumberValue(creationVo.studentNumber()))
                .grade(creationVo.grade().grade())
                .role(extractRoleValue(creationVo.role()))
                .major(extractMajorValue(creationVo.major()))
                .skills(extractSkillsArray(creationVo.skills()))
                .description(creationVo.description())

                .profileImage(extractProfileImageValue(creationVo.profileImage()))

                // 기본값 필드 (현재 DB 스키마 호환성)
                .birthday(OffsetDateTime.now().minusYears(20)) // 기본 나이 20세로 설정
                .gender("UNKNOWN") // 기본 성별

                // 자동 관리 필드 (JPA에서 자동 설정)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * MemberCreationVo → MemberEntity 변환 (생년월일, 성별 포함)
     * 추가 정보와 함께 회원 생성 시 사용
     */
    public MemberEntity toEntity(MemberCreationVo creationVo, BirthdayVo birthday, GenderVo gender) {
        return MemberEntity.builder()
                .name(creationVo.name().value())
                .studentNumber(creationVo.studentNumber().value())
                .grade(creationVo.grade().grade())
                .role(creationVo.role().role())
                .major(creationVo.major().value())
                .skills(convertSkillsToArray(creationVo.skills()))
                .description(creationVo.description())
                .profileImage(creationVo.profileImage() != null ?
                        creationVo.profileImage().value() : null)
                .birthday(
                        birthday != null
                                ? birthday.value()
                                : OffsetDateTime.now(ZoneOffset.of("+09:00")).minusYears(20)
                )
                .gender(gender != null ? gender.value() : "UNKNOWN")
                .build();
    }

    /**
     * MemberUpdateVo로 기존 MemberEntity 업데이트
     * Repository에서 Member 수정 시 사용 (빌더 패턴)
     */
    public MemberEntity updateEntity(MemberEntity existingEntity, MemberUpdateVo updateVo) {
        // toBuilder()를 사용하여 기존 값들을 유지하면서 업데이트
        // updatedAt은 @UpdateTimestamp에 의해 자동 갱신됨
        return existingEntity.toBuilder()
                .name(updateVo.name() != null ? updateVo.name().value() : existingEntity.getName())
                .grade(updateVo.grade() != null ? updateVo.grade().grade() : existingEntity.getGrade())
                .role(updateVo.role() != null ? updateVo.role().role() : existingEntity.getRole())
                .major(updateVo.major() != null ? updateVo.major().value() : existingEntity.getMajor())
                .skills(updateVo.skills() != null ? convertSkillsToArray(updateVo.skills()) : existingEntity.getSkills())
                .description(updateVo.description() != null ? updateVo.description() : existingEntity.getDescription())
                .build();
    }

    /**
     * ProfileUpdateVo로 기존 MemberEntity 프로필 업데이트
     * Repository에서 Member 프로필 수정 시 사용 (빌더 패턴)
     */
    public MemberEntity updateProfileEntity(MemberEntity existingEntity, ProfileUpdateVo profileUpdateVo) {
        return existingEntity.toBuilder()
                .name(profileUpdateVo.name() != null ? profileUpdateVo.name().value() : existingEntity.getName())
                .profileImage(profileUpdateVo.profileImage() != null ?
                        profileUpdateVo.profileImage().value() : existingEntity.getProfileImage())
                .build();
    }

    /**
     * 개별 VO들로부터 MemberEntity 생성 (재구성용)
     * Repository에서 완전한 Member 재구성 시 사용 (빌더 패턴)
     */
    public MemberEntity toEntity(Long id, NameVo name, StudentNumberVo studentNumber,
                                 ProfileImageVo profileImage, GradeVo grade, RoleVo role,
                                 SkillsVo skills, MajorVo major, String description,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return MemberEntity.builder()
                .id(id)
                .name(name.value())
                .studentNumber(studentNumber.value())
                .profileImage(profileImage != null ? profileImage.value() : null)
                .grade(grade.grade())
                .role(role.role())
                .skills(convertSkillsToArray(skills))
                .major(major.value())
                .description(description)
                .birthday(OffsetDateTime.now().minusYears(20)) // 기본값
                .gender("UNKNOWN") // 기본값
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * 완전한 정보로 MemberEntity 생성 (모든 필드 포함, 빌더 패턴)
     */
    public MemberEntity toEntity(Long id, NameVo name, StudentNumberVo studentNumber,
                                 ProfileImageVo profileImage, GradeVo grade, RoleVo role,
                                 SkillsVo skills, MajorVo major, String description,
                                 BirthdayVo birthday, GenderVo gender,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return MemberEntity.builder()
                .id(id)
                .name(name.value())
                .studentNumber(studentNumber.value())
                .profileImage(profileImage != null ? profileImage.value() : null)
                .grade(grade.grade())
                .role(role.role())
                .skills(convertSkillsToArray(skills))
                .major(major.value())
                .description(description)
                .birthday(
                        birthday != null
                                ? birthday.value()
                                : OffsetDateTime.now(ZoneOffset.of("+09:00")).minusYears(20)
                )
                .gender(gender != null ? gender.value() : "UNKNOWN")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // =================================================================
    // 검색/필터링 조건 → JPA 쿼리 조건 변환
    // =================================================================

    /**
     * MemberSearchConditionVo → JPA 쿼리 조건 변환
     * QueryDSL이나 Criteria API에서 사용할 조건들로 변환
     */
    public SearchConditionJpa toSearchCondition(MemberSearchConditionVo searchConditionVo) {
        return SearchConditionJpa.builder()
                .keyword(searchConditionVo.keyword())
                .grade(searchConditionVo.grade() != null ? searchConditionVo.grade().grade() : null)
                .role(searchConditionVo.role() != null ? searchConditionVo.role().role() : null)
                .major(searchConditionVo.major() != null ? searchConditionVo.major().value() : null)
                .skills(searchConditionVo.skills() != null ?
                        convertSkillsToArray(searchConditionVo.skills()) : null)
                .build();
    }

    /**
     * MemberFilterVo → JPA 필터 조건 변환
     */
    public FilterConditionJpa toFilterCondition(MemberFilterVo filterVo) {
        return FilterConditionJpa.builder()
                .grade(filterVo.grade() != null ? filterVo.grade().grade().toString() : null)
                .role(filterVo.role() != null ? filterVo.role().role() : null)
                .major(filterVo.major() != null ? filterVo.major().value() : null)
                .isActive(filterVo.isActive())
                .build();
    }

    // =================================================================
    // 헬퍼 메서드들
    // =================================================================

    /**
     * SkillsVo를 String[] 배열로 변환
     * MemberEntity에서 기술 스택을 String[] 배열로 저장하는 경우
     */
    private String[] convertSkillsToArray(SkillsVo skillsVo) {
        if (skillsVo == null || skillsVo.values().isEmpty()) {
            return new String[0];
        }

        return skillsVo.values().stream()
                .filter(skill -> skill != null && !skill.trim().isEmpty())
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * List<String>을 String[] 배열로 변환
     */
    private String[] convertSkillsListToArray(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return new String[0];
        }

        return skills.stream()
                .filter(skill -> skill != null && !skill.trim().isEmpty())
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * String[] 배열을 검색용 문자열로 변환
     */
    private String joinSkillsArray(String[] skillsArray) {
        if (skillsArray == null || skillsArray.length == 0) {
            return "";
        }

        return Arrays.stream(skillsArray)
                .filter(skill -> skill != null && !skill.trim().isEmpty())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    // =================================================================
    // VO → Primitive 값 추출 헬퍼 메서드들
    // 각 VO에서 안전하게 primitive 값을 추출
    // =================================================================

    /**
     * NameVo → String 추출
     */
    private String extractNameValue(NameVo nameVo) {
        if (nameVo == null) {
            throw new IllegalArgumentException("NameVo는 필수입니다");
        }
        return nameVo.value();
    }

    /**
     * StudentNumberVo → String 추출
     */
    private String extractStudentNumberValue(StudentNumberVo studentNumberVo) {
        if (studentNumberVo == null) {
            throw new IllegalArgumentException("StudentNumberVo는 필수입니다");
        }
        return studentNumberVo.value();
    }

    /**
     * GradeVo → String 추출
     */
    private MemberGrade extractGradeValue(GradeVo gradeVo) {
        if (gradeVo == null) {
            throw new IllegalArgumentException("GradeVo는 필수입니다");
        }
        return gradeVo.grade();
    }

    /**
     * RoleVo → MemberRole 추출
     */
    private MemberRole extractRoleValue(RoleVo roleVo) {
        if (roleVo == null) {
            throw new IllegalArgumentException("RoleVo는 필수입니다");
        }
        return roleVo.role();
    }

    /**
     * MajorVo → String 추출
     */
    private String extractMajorValue(MajorVo majorVo) {
        if (majorVo == null) {
            throw new IllegalArgumentException("MajorVo는 필수입니다");
        }
        return majorVo.value();
    }

    /**
     * SkillsVo → String[] 추출 (PostgreSQL Array 타입)
     */
    private String[] extractSkillsArray(SkillsVo skillsVo) {
        if (skillsVo == null) {
            return new String[0]; // 빈 배열 반환
        }

        List<String> values = skillsVo.values();
        if (values == null) {
            return new String[0];
        }
        return values.toArray(new String[0]);
    }

    /**
     * EmailVo → String 추출 (선택적)
     */
    private String extractEmailValue(EmailVo emailVo) {
        return emailVo != null ? emailVo.value() : null;
    }

    /**
     * ProfileImageVo → String 추출 (선택적)
     */
    private String extractProfileImageValue(ProfileImageVo profileImageVo) {
        return profileImageVo != null ? profileImageVo.value() : null;
    }

    // =================================================================
    // 내부 클래스들 (JPA 쿼리용 조건 객체들)
    // =================================================================

    /**
     * JPA 검색 조건용 내부 클래스
     */
    public static class SearchConditionJpa {
        private final String keyword;
        private final MemberGrade grade;
        private final MemberRole role;
        private final String major;
        private final String[] skills;

        private SearchConditionJpa(Builder builder) {
            this.keyword = builder.keyword;
            this.grade = builder.grade;
            this.role = builder.role;
            this.major = builder.major;
            this.skills = builder.skills;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getKeyword() { return keyword; }
        public MemberGrade getGrade() { return grade; }
        public MemberRole getRole() { return role; }
        public String getMajor() { return major; }
        public String[] getSkills() { return skills; }

        public static class Builder {
            private String keyword;
            private MemberGrade grade;
            private MemberRole role;
            private String major;
            private String[] skills;

            public Builder keyword(String keyword) { this.keyword = keyword; return this; }
            public Builder grade(MemberGrade grade) { this.grade = grade; return this; }
            public Builder role(MemberRole role) { this.role = role; return this; }
            public Builder major(String major) { this.major = major; return this; }
            public Builder skills(String[] skills) { this.skills = skills; return this; }

            public SearchConditionJpa build() {
                return new SearchConditionJpa(this);
            }
        }
    }

    /**
     * JPA 필터 조건용 내부 클래스
     */
    public static class FilterConditionJpa {
        private final String grade;
        private final MemberRole role;
        private final String major;
        private final Boolean isActive;

        private FilterConditionJpa(Builder builder) {
            this.grade = builder.grade;
            this.role = builder.role;
            this.major = builder.major;
            this.isActive = builder.isActive;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getGrade() { return grade; }
        public MemberRole getRole() { return role; }
        public String getMajor() { return major; }
        public Boolean getIsActive() { return isActive; }

        public static class Builder {
            private String grade;
            private MemberRole role;
            private String major;
            private Boolean isActive;

            public Builder grade(String grade) { this.grade = grade; return this; }
            public Builder role(MemberRole role) { this.role = role; return this; }
            public Builder major(String major) { this.major = major; return this; }
            public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }

            public FilterConditionJpa build() {
                return new FilterConditionJpa(this);
            }
        }
    }


// MemberContactVo → MemberContactEntity 변환
    public MemberContactEntity toEntity(MemberContactVo contactVo) {

        return MemberContactEntity.builder()
                .memberId(contactVo.memberId().value())
                .email(extractEmailValue(contactVo.email()))
                .phoneNumber(contactVo.phoneNumber().value())
                .githubUrl(null)     // 초기 회원가입 시점에서는 null
                .linkedinUrl(null)   // 초기 회원가입 시점에서는 null
                .build();
    }
}