package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Primitive To VO Mapper
 * 
 * ✅ Primitive/Reference Type → VO 변환 담당
 * ✅ Domain Layer의 primitive → VO 전용 매퍼
 * ✅ 네이밍 컨벤션: PrimitiveToVoMapper
 */
@Component
@RequiredArgsConstructor
public class PrimitiveToVoMapper {

    /**
     * String → NameVo 변환
     */
    public NameVo toNameVo(String name) {
        return NameVo.of(name);
    }

    /**
     * String → StudentNumberVo 변환
     */
    public StudentNumberVo toStudentNumberVo(String studentNumber) {
        return new StudentNumberVo(studentNumber);
    }

    /**
     * String → EmailVo 변환
     */
    public EmailVo toEmailVo(String email) {
        return email != null ? new EmailVo(email) : null;
    }

    /**
     * String → GradeVo 변환
     */
    public GradeVo toGradeVo(String grade) {
        return GradeVo.of(grade);
    }

    /**
     * String → RoleVo 변환
     */
    public RoleVo toRoleVo(String role) {
        return RoleVo.of(role);
    }

    /**
     * String → MajorVo 변환
     */
    public MajorVo toMajorVo(String major) {
        return MajorVo.of(major);
    }

    /**
     * String → ProfileImageVo 변환
     */
    public ProfileImageVo toProfileImageVo(String profileImage) {
        return profileImage != null ? new ProfileImageVo(profileImage) : null;
    }

    /**
     * List<String> → SkillsVo 변환
     */
    public SkillsVo toSkillsVo(List<String> skills) {
        return new SkillsVo(skills);
    }

    /**
     * Long → MemberIdVo 변환
     */
    public MemberIdVo toMemberIdVo(Long memberId) {
        return new MemberIdVo(memberId);
    }

    /**
     * LocalDateTime → BirthdayVo 변환
     */
    public BirthdayVo toBirthdayVo(LocalDateTime birthday) {
        return birthday != null ? new BirthdayVo(birthday) : null;
    }

    /**
     * String → GenderVo 변환
     */
    public GenderVo toGenderVo(String gender) {
        return gender != null ? new GenderVo(gender) : null;
    }
} 