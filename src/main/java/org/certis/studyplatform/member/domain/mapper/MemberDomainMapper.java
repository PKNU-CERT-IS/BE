package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Member Domain Mapper (Facade)
 * 
 * ✅ Domain Layer의 통합 매퍼 (Facade Pattern)
 * ✅ PrimitiveToVoMapper, VoToDomainMapper, DomainToVoMapper를 통합 관리
 * ✅ 네이밍 컨벤션: MemberDomainMapper
 */
@Component
@RequiredArgsConstructor
public class MemberDomainMapper {

    private final PrimitiveToVoMapper primitiveToVoMapper;
    private final VoToDomainMapper voToDomainMapper;
    private final DomainToVoMapper domainToVoMapper;

    // =================================================================
    // Primitive → VO 변환 (PrimitiveToVoMapper 위임)
    // =================================================================

    public NameVo toNameVo(String name) {
        return primitiveToVoMapper.toNameVo(name);
    }

    public StudentNumberVo toStudentNumberVo(String studentNumber) {
        return primitiveToVoMapper.toStudentNumberVo(studentNumber);
    }

    public EmailVo toEmailVo(String email) {
        return primitiveToVoMapper.toEmailVo(email);
    }

    public GradeVo toGradeVo(String grade) {
        return primitiveToVoMapper.toGradeVo(grade);
    }

    public RoleVo toRoleVo(String role) {
        return primitiveToVoMapper.toRoleVo(role);
    }

    public MajorVo toMajorVo(String major) {
        return primitiveToVoMapper.toMajorVo(major);
    }

    public ProfileImageVo toProfileImageVo(String profileImage) {
        return primitiveToVoMapper.toProfileImageVo(profileImage);
    }

    public SkillsVo toSkillsVo(List<String> skills) {
        return primitiveToVoMapper.toSkillsVo(skills);
    }

    public MemberIdVo toMemberIdVo(Long memberId) {
        return primitiveToVoMapper.toMemberIdVo(memberId);
    }

    // =================================================================
    // VO → Domain 변환 (VoToDomainMapper 위임)
    // =================================================================

    public Member toMember(String name, String studentNumber, String grade,
                          String role, String major, String description, 
                          List<String> skills) {
        return voToDomainMapper.toMember(name, studentNumber, grade, role, major, description, skills);
    }

    public Member toMember(Long id, NameVo name, StudentNumberVo studentNumber, 
                          ProfileImageVo profileImage, GradeVo grade, RoleVo role, 
                          SkillsVo skills, MajorVo major, String description,
                          ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return voToDomainMapper.toMember(id, name, studentNumber, profileImage, grade, role, 
                                       skills, major, description, createdAt, updatedAt);
    }

    public Member toMember(String name, String studentNumber, String grade,
                          List<String> skills, String role, String major) {
        return voToDomainMapper.toMember(name, studentNumber, grade, skills, role, major);
    }

    // =================================================================
    // Domain → VO 변환 (DomainToVoMapper 위임)
    // =================================================================

    public MemberVo toMemberVo(Member member) {
        return domainToVoMapper.toMemberVo(member);
    }

    public MemberCreatedVo toMemberCreatedVo(Member member) {
        return domainToVoMapper.toMemberCreatedVo(member);
    }

    public MemberUpdatedVo toMemberUpdatedVo(Member member) {
        return domainToVoMapper.toMemberUpdatedVo(member);
    }

    public MemberSummaryVo toMemberSummaryVo(Member member) {
        return domainToVoMapper.toMemberSummaryVo(member);
    }

    public MemberIdVo toMemberIdVo(Member member) {
        return domainToVoMapper.toMemberIdVo(member);
    }

    public NameVo toNameVo(Member member) {
        return domainToVoMapper.toNameVo(member);
    }

    public StudentNumberVo toStudentNumberVo(Member member) {
        return domainToVoMapper.toStudentNumberVo(member);
    }

    public GradeVo toGradeVo(Member member) {
        return domainToVoMapper.toGradeVo(member);
    }

    public RoleVo toRoleVo(Member member) {
        return domainToVoMapper.toRoleVo(member);
    }

    public MajorVo toMajorVo(Member member) {
        return domainToVoMapper.toMajorVo(member);
    }

    public SkillsVo toSkillsVo(Member member) {
        return domainToVoMapper.toSkillsVo(member);
    }

    public ProfileImageVo toProfileImageVo(Member member) {
        return domainToVoMapper.toProfileImageVo(member);
    }
}
