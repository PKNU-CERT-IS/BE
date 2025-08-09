package org.certis.studyplatform.member.infrastructure.mapper;

import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Member Infrastructure VO Mapper
 *
 * ✅ Infrastructure Layer의 MemberEntity → Domain VO 변환 전용 매퍼
 * ✅ Repository 구현체에서 조회 결과를 Domain VO로 변환할 때 사용
 * ✅ MemberEntity → Domain VO 변환 담당
 *
 * 책임:
 * - MemberEntity → Domain VO 변환 (Repository 조회 시)
 * - Database 결과를 Domain Layer가 이해하는 VO로 변환
 * - Infrastructure 계층의 데이터를 Domain 계층으로 전달
 *
 * 사용처:
 * - MemberRepositoryImpl (JPA Repository 구현체)
 * - MemberQueryRepositoryImpl
 * - Infrastructure Layer의 Entity → VO 변환 로직
 */
@Component
public class MemberInfrastructureVoMapper {

    // =================================================================
    // MemberEntity → Domain VO 변환 (Repository 조회 시 사용)
    // =================================================================

    /**
     * MemberEntity → MemberVo 변환
     * 완전한 회원 정보를 포함한 VO 생성
     */
    public MemberVo toMemberVo(MemberEntity entity) {
        return new MemberVo(
                entity.getId(),
                entity.getName(),
                entity.getStudentNumber(),
                entity.getProfileImage(),
                entity.getGrade(),
                entity.getRole(),
                parseSkillsFromArray(entity.getSkills()).values(),
                entity.getMajor(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * MemberEntity → MemberCreatedVo 변환
     * 회원 생성 응답용 VO 생성
     */
    public MemberCreatedVo toMemberCreatedVo(MemberEntity entity) {
        return new MemberCreatedVo(
                MemberIdVo.of(entity.getId()),
                new StudentNumberVo(entity.getStudentNumber())
        );
    }

    /**
     * MemberEntity → MemberUpdatedVo 변환
     * 회원 수정 응답용 VO 생성
     */
    public MemberUpdatedVo toMemberUpdatedVo(MemberEntity entity) {
        return new MemberUpdatedVo(
                MemberIdVo.of(entity.getId()),
                NameVo.of(entity.getName()),
                entity.getProfileImage() != null ? ProfileImageVo.of(entity.getProfileImage()) : null,
                entity.getUpdatedAt()
        );
    }

    /**
     * MemberEntity → MemberSummaryVo 변환
     * 회원 목록 조회용 요약 VO 생성
     */
    public MemberSummaryVo toMemberSummaryVo(MemberEntity entity) {
        return new MemberSummaryVo(
                MemberIdVo.of(entity.getId()),
                NameVo.of(entity.getName()),
                new StudentNumberVo(entity.getStudentNumber()),
                GradeVo.of(entity.getGrade()),
                RoleVo.of(entity.getRole()),
                MajorVo.of(entity.getMajor()),
                entity.getDescription(),
                parseSkillsFromArray(entity.getSkills()),
                entity.getProfileImage() != null ? ProfileImageVo.of(entity.getProfileImage()) : null,
                entity.getCreatedAt()
        );
    }

    // =================================================================
    // 개별 VO 변환 메서드들 (필요시 사용)
    // =================================================================

    /**
     * MemberEntity → MemberIdVo 변환
     */
    public MemberIdVo toMemberIdVo(MemberEntity entity) {
        return MemberIdVo.of(entity.getId());
    }

    /**
     * MemberEntity → NameVo 변환
     */
    public NameVo toNameVo(MemberEntity entity) {
        return NameVo.of(entity.getName());
    }

    /**
     * MemberEntity → StudentNumberVo 변환
     */
    public StudentNumberVo toStudentNumberVo(MemberEntity entity) {
        return new StudentNumberVo(entity.getStudentNumber());
    }

    /**
     * MemberEntity → GradeVo 변환
     */
    public GradeVo toGradeVo(MemberEntity entity) {
        return GradeVo.of(entity.getGrade());
    }

    /**
     * MemberEntity → RoleVo 변환
     */
    public RoleVo toRoleVo(MemberEntity entity) {
        return RoleVo.of(entity.getRole());
    }

    /**
     * MemberEntity → MajorVo 변환
     */
    public MajorVo toMajorVo(MemberEntity entity) {
        return MajorVo.of(entity.getMajor());
    }

    /**
     * MemberEntity → SkillsVo 변환
     */
    public SkillsVo toSkillsVo(MemberEntity entity) {
        return parseSkillsFromArray(entity.getSkills());
    }

    /**
     * MemberEntity → ProfileImageVo 변환
     */
    public ProfileImageVo toProfileImageVo(MemberEntity entity) {
        return entity.getProfileImage() != null ?
                ProfileImageVo.of(entity.getProfileImage()) : null;
    }

    /**
     * MemberEntity → BirthdayVo 변환
     * 추가 필드 지원
     */
    public BirthdayVo toBirthdayVo(MemberEntity entity) {
        return entity.getBirthday() != null ?
                BirthdayVo.of(entity.getBirthday().toLocalDateTime()) : null;
    }

    /**
     * MemberEntity → GenderVo 변환
     * 추가 필드 지원
     */
    public GenderVo toGenderVo(MemberEntity entity) {
        return entity.getGender() != null ?
                GenderVo.of(entity.getGender()) : null;
    }

    // =================================================================
    // 헬퍼 메서드들
    // =================================================================

    /**
     * 기술 스택 배열을 SkillsVo로 파싱
     * MemberEntity의 skills가 String[] 배열로 저장되어 있는 경우
     */
    private SkillsVo parseSkillsFromArray(String[] skillsArray) {
        if (skillsArray == null || skillsArray.length == 0) {
            return new SkillsVo(List.of());
        }

        List<String> skillsList = Arrays.stream(skillsArray)
                .filter(skill -> skill != null && !skill.trim().isEmpty())
                .map(String::trim)
                .toList();

        return new SkillsVo(skillsList);
    }

    /**
     * 기술 스택 배열의 개수 반환
     */
    public int getSkillsCount(MemberEntity entity) {
        return entity.getSkills() != null ? entity.getSkills().length : 0;
    }

    /**
     * 활성 상태 확인 (삭제되지 않은 상태)
     */
    public boolean isActive(MemberEntity entity) {
        return entity.getDeletedAt() == null;
    }
}