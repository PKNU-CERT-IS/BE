package org.certis.studyplatform.member.infrastructure.mapper;

import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class MemberMapper {

    // =================================================================
    // Public Methods
    // =================================================================

    /**
     * Domain Member -> MemberEntity 변환 (메인 메서드)
     */
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
     */
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


    // =================================================================
    // Private Helper Methods: Domain -> Entity
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

    private MemberRole mapRoleVoToString(MemberRole roleVo) {
        return roleVo;
    }

    private String mapMajorVoToString(MajorVo majorVo) {
        return majorVo != null ? majorVo.value() : null;
    }

    private String[] mapSkillsVoToStringArray(SkillsVo skillsVo) {
        if (skillsVo == null || skillsVo.values() == null) {
            return null;
        }
        return skillsVo.values().toArray(new String[0]);
    }

    // =================================================================
    // Private Helper Methods: Entity -> Domain
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

    private MemberRole mapStringToRoleVo(MemberRole role) {
        return role ;
    }

    private MajorVo mapStringToMajorVo(String major) {
        return major != null ? MajorVo.of(major) : null;
    }

    /**
     * Entity의 String 배열을 SkillsVo로 변환합니다.
     * 배열이 비어있거나 null이면, SkillsVo의 '비어있을 수 없다'는 도메인 규칙을 준수하기 위해 null을 반환합니다.
     */
    private SkillsVo mapStringArrayToSkillsVo(String[] skills) {
        if (skills == null || skills.length == 0) {
            return null;
        }
        return SkillsVo.of(Arrays.asList(skills));
    }

    public List<String> parseSkillsFromDatabase(Object o) {
        return  new ArrayList<>();
    }
}
