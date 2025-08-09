package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Member Domain Mapper (Facade)
 *
 * ✅ Domain Layer의 Command/Query → VO 변환 전용 매퍼
 * ✅ Domain Service에서 Command/Query 객체를 VO로 변환할 때 사용
 * ✅ Entity to VO 변환은 Infrastructure Layer에서 담당
 *
 * 책임:
 * - Command Object → VO 변환 (검증 포함)
 * - Query Object → VO 변환 (검증 포함)
 *
 * 제외사항:
 * - Entity → VO 변환 (Infrastructure 담당)
 * - VO → Entity 변환 (Infrastructure 담당)
 * - Domain Entity 관련 변환 (Infrastructure 담당)
 *
 * 특징:
 * - Command/Query 객체의 primitive 값들을 검증된 VO로 변환
 * - VO 생성 시점에서 자동으로 비즈니스 검증 수행
 * - 단방향 데이터 흐름에서 변환 역할 담당
 */
@Component
@RequiredArgsConstructor
public class MemberDomainMapper {

    private final MemberDomainCommandMapper memberDomainCommandMapper;
    private final MemberDomainQueryMapper memberDomainQueryMapper;

    // =================================================================
    // Command Object → VO 변환 (Domain Service에서 사용)
    // Command 객체의 primitive 값들을 검증이 포함된 VO로 변환
    // =================================================================

    /**
     * String → NameVo 변환 (검증 포함)
     */
    public NameVo toNameVo(String name) {
        return memberDomainCommandMapper.toNameVo(name);
    }

    /**
     * String → StudentNumberVo 변환 (검증 포함)
     */
    public StudentNumberVo toStudentNumberVo(String studentNumber) {
        return memberDomainCommandMapper.toStudentNumberVo(studentNumber);
    }

    /**
     * String → EmailVo 변환 (검증 포함)
     */
    public EmailVo toEmailVo(String email) {
        return memberDomainCommandMapper.toEmailVo(email);
    }

    /**
     * String → GradeVo 변환 (검증 포함)
     */
    public GradeVo toGradeVo(String grade) {
        return memberDomainCommandMapper.toGradeVo(grade);
    }

    /**
     * String → RoleVo 변환 (검증 포함)
     */
    public RoleVo toRoleVo(MemberRole role) {
        return memberDomainCommandMapper.toRoleVo(role);
    }

    /**
     * String → MajorVo 변환 (검증 포함)
     */
    public MajorVo toMajorVo(String major) {
        return memberDomainCommandMapper.toMajorVo(major);
    }

    /**
     * String → ProfileImageVo 변환 (검증 포함)
     */
    public ProfileImageVo toProfileImageVo(String profileImage) {
        return memberDomainCommandMapper.toProfileImageVo(profileImage);
    }

    /**
     * List<String> → SkillsVo 변환 (검증 포함)
     */
    public SkillsVo toSkillsVo(List<String> skills) {
        return memberDomainCommandMapper.toSkillsVo(skills);
    }

    /**
     * Long → MemberIdVo 변환 (검증 포함)
     */
    public MemberIdVo toMemberIdVo(Long memberId) {
        return memberDomainCommandMapper.toMemberIdVo(memberId);
    }

    // =================================================================
    // Query Object → VO 변환 (필요시 확장)
    // Query 객체의 검색 조건들을 VO로 변환
    // =================================================================

    /**
     * Query 조건을 검색용 VO로 변환 (필요시 구현)
     */
    public MemberIdVo toSearchMemberIdVo(Long memberId) {
        return memberDomainQueryMapper.toSearchMemberIdVo(memberId);
    }

    /**
     * Query 조건을 필터링용 VO로 변환 (필요시 구현)
     */
    public GradeVo toSearchGradeVo(String grade) {
        return memberDomainQueryMapper.toSearchGradeVo(grade);
    }

    /**
     * Query 조건을 필터링용 VO로 변환 (필요시 구현)
     */
    public RoleVo toSearchRoleVo(MemberRole role) {
        return memberDomainQueryMapper.toSearchRoleVo(role);
    }
}