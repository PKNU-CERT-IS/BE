package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Primitive To VO Mapper
 *
 * ✅ Command/Query 객체의 Primitive/Reference Type → VO 변환 담당
 * ✅ Domain Service에서 Command → VO 변환 시 사용
 * ✅ VO 생성 시점에서 자동으로 비즈니스 검증 수행
 * ✅ 네이밍 컨벤션: PrimitiveToVoMapper
 *
 * 특징:
 * - Command 객체의 primitive 값들을 검증된 VO로 변환
 * - VO 생성자/팩토리 메서드에서 비즈니스 검증 자동 수행
 * - 단방향 데이터 흐름에서 검증 게이트웨이 역할
 */
@Component
@RequiredArgsConstructor
public class MemberDomainCommandMapper {

    /**
     * String → NameVo 변환
     * VO 생성 시 이름 검증 규칙 자동 적용
     */
    public NameVo toNameVo(String name) {
        // NameVo.of() 내부에서 검증 수행 (null 체크, 길이 검증, 형식 검증 등)
        return NameVo.of(name);
    }

    /**
     * String → StudentNumberVo 변환
     * VO 생성 시 학번 검증 규칙 자동 적용
     */
    public StudentNumberVo toStudentNumberVo(String studentNumber) {
        // StudentNumberVo 생성자에서 검증 수행 (null 체크, 형식 검증 등)
        return new StudentNumberVo(studentNumber);
    }

    /**
     * String → EmailVo 변환
     * VO 생성 시 이메일 검증 규칙 자동 적용
     */
    public EmailVo toEmailVo(String email) {
        // EmailVo 생성자에서 검증 수행 (null 허용, 이메일 형식 검증 등)
        return email != null ? new EmailVo(email) : null;
    }

    /**
     * String → GradeVo 변환
     * VO 생성 시 학년 검증 규칙 자동 적용
     */
    public GradeVo toGradeVo(String grade) {
        // GradeVo.of() 내부에서 검증 수행 (null 체크, 범위 검증 등)
        return GradeVo.of(grade);
    }

    /**
     * String → RoleVo 변환
     * VO 생성 시 역할 검증 규칙 자동 적용
     */
    public RoleVo toRoleVo(MemberRole role) {
        // RoleVo.of() 내부에서 검증 수행 (null 체크, 허용된 역할 검증 등)
        return RoleVo.of(role);
    }

    /**
     * String → MajorVo 변환
     * VO 생성 시 전공 검증 규칙 자동 적용
     */
    public MajorVo toMajorVo(String major) {
        // MajorVo.of() 내부에서 검증 수행 (null 체크, 허용된 전공 검증 등)
        return MajorVo.of(major);
    }

    /**
     * String → ProfileImageVo 변환
     * VO 생성 시 프로필 이미지 검증 규칙 자동 적용
     */
    public ProfileImageVo toProfileImageVo(String profileImage) {
        // ProfileImageVo 생성자에서 검증 수행 (null 허용, URL 형식 검증 등)
        return profileImage != null ? new ProfileImageVo(profileImage) : null;
    }

    /**
     * List<String> → SkillsVo 변환
     * VO 생성 시 기술 스택 검증 규칙 자동 적용
     */
    public SkillsVo toSkillsVo(List<String> skills) {
        // SkillsVo 생성자에서 검증 수행 (null 체크, 리스트 크기 검증, 중복 제거 등)
        return new SkillsVo(skills);
    }

    /**
     * Long → MemberIdVo 변환
     * VO 생성 시 회원 ID 검증 규칙 자동 적용
     */
    public MemberIdVo toMemberIdVo(Long memberId) {
        // MemberIdVo 생성자에서 검증 수행 (null 체크, 양수 검증 등)
        return new MemberIdVo(memberId);
    }

    /**
     * LocalDateTime → BirthdayVo 변환
     * VO 생성 시 생년월일 검증 규칙 자동 적용
     */
    public BirthdayVo toBirthdayVo(OffsetDateTime birthday) {
        // BirthdayVo 생성자에서 검증 수행 (null 허용, 날짜 범위 검증 등)
        return birthday != null ? new BirthdayVo(birthday) : null;
    }

    /**
     * String → GenderVo 변환
     * VO 생성 시 성별 검증 규칙 자동 적용
     */
    public GenderVo toGenderVo(String gender) {
        // GenderVo 생성자에서 검증 수행 (null 허용, 허용된 성별 값 검증 등)
        return gender != null ? new GenderVo(gender) : null;
    }
}