package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

/**
 * Domain To VO Mapper
 * 
 * ✅ Domain Entity → VO 변환 담당
 * ✅ Domain Layer의 Domain → VO 전용 매퍼
 * ✅ 네이밍 컨벤션: DomainToVoMapper
 */
@Component
@RequiredArgsConstructor
public class DomainToVoMapper {

    /**
     * Member Domain Entity → MemberVo 변환
     */
    public MemberVo toMemberVo(Member member) {
        return new MemberVo(
            member.getId(),
            member.getNameValue(),
            member.getStudentNumberValue(),
            member.getProfileImageValue(),
            member.getGradeValue(),
            member.getRoleValue(),
            member.getSkills().values(),
            member.getMajorValue(),
            member.getDescription(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }

    /**
     * Member Domain Entity → MemberCreatedVo 변환
     */
    public MemberCreatedVo toMemberCreatedVo(Member member) {
        return new MemberCreatedVo(
            member.getId(),
            member.getNameValue(),
            member.getStudentNumberValue(),
            member.getCreatedAt()
        );
    }

    /**
     * Member Domain Entity → MemberUpdatedVo 변환
     */
    public MemberUpdatedVo toMemberUpdatedVo(Member member) {
        return new MemberUpdatedVo(
            member.getId(),
            member.getNameValue(),
            member.getProfileImageValue(),
            member.getUpdatedAt()
        );
    }

    /**
     * Member Domain Entity → MemberSummaryVo 변환
     */
    public MemberSummaryVo toMemberSummaryVo(Member member) {
        return new MemberSummaryVo(
            member.getId(),
            member.getNameValue(),
            member.getStudentNumberValue(),
            member.getGradeValue(),
            member.getRoleValue(),
            member.getMajorValue(),
            member.getDescription(),
            member.getSkills().values(),
            member.getProfileImageValue(),
            member.getCreatedAt()
        );
    }

    /**
     * Member Domain Entity → MemberIdVo 변환
     */
    public MemberIdVo toMemberIdVo(Member member) {
        return new MemberIdVo(member.getId());
    }

    /**
     * Member Domain Entity → NameVo 변환
     */
    public NameVo toNameVo(Member member) {
        return member.getName();
    }

    /**
     * Member Domain Entity → StudentNumberVo 변환
     */
    public StudentNumberVo toStudentNumberVo(Member member) {
        return member.getStudentNumber();
    }

    /**
     * Member Domain Entity → GradeVo 변환
     */
    public GradeVo toGradeVo(Member member) {
        return member.getGrade();
    }

    /**
     * Member Domain Entity → RoleVo 변환
     */
    public RoleVo toRoleVo(Member member) {
        return member.getRole();
    }

    /**
     * Member Domain Entity → MajorVo 변환
     */
    public MajorVo toMajorVo(Member member) {
        return member.getMajor();
    }

    /**
     * Member Domain Entity → SkillsVo 변환
     */
    public SkillsVo toSkillsVo(Member member) {
        return member.getSkills();
    }

    /**
     * Member Domain Entity → ProfileImageVo 변환
     */
    public ProfileImageVo toProfileImageVo(Member member) {
        return member.getProfileImage();
    }
} 