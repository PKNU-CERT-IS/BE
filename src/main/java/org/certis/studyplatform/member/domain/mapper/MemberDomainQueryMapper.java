package org.certis.studyplatform.member.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Component;

/**
 * Query Object To VO Mapper
 *
 * ✅ Query Object → VO 변환 담당
 * ✅ Domain Service에서 Query 객체의 조건들을 VO로 변환할 때 사용
 * ✅ 검색 조건, 필터링 조건을 검증된 VO로 변환
 * ✅ 네이밍 컨벤션: QueryObjectToVoMapper
 *
 * 특징:
 * - Query 객체의 검색/필터링 조건을 VO로 변환
 * - VO 생성 시점에서 검색 조건 검증 수행
 * - Infrastructure의 Entity 변환과는 별개로 동작
 * - Domain Service의 Query 처리 시 사용
 */
@Component
@RequiredArgsConstructor
public class MemberDomainQueryMapper {

    /**
     * Long → MemberIdVo 변환 (검색용)
     * Query에서 회원 ID 조건을 VO로 변환
     */
    public MemberIdVo toSearchMemberIdVo(Long memberId) {
        // MemberIdVo 생성자에서 검증 수행 (null 체크, 양수 검증 등)
        return new MemberIdVo(memberId);
    }

    /**
     * String → GradeVo 변환 (필터링용)
     * Query에서 학년 필터 조건을 VO로 변환
     */
    public GradeVo toSearchGradeVo(String grade) {
        // GradeVo.of() 내부에서 검증 수행 (null 체크, 범위 검증 등)
        return grade != null ? GradeVo.of(grade) : null;
    }

    /**
     * String → RoleVo 변환 (필터링용)
     * Query에서 역할 필터 조건을 VO로 변환
     */
    public RoleVo toSearchRoleVo(MemberRole role) {
        // RoleVo.of() 내부에서 검증 수행 (null 체크, 허용된 역할 검증 등)
        return role != null ? RoleVo.of(role) : null;
    }

    /**
     * String → MajorVo 변환 (필터링용)
     * Query에서 전공 필터 조건을 VO로 변환
     */
    public MajorVo toSearchMajorVo(String major) {
        // MajorVo.of() 내부에서 검증 수행 (null 체크, 허용된 전공 검증 등)
        return major != null ? MajorVo.of(major) : null;
    }

    /**
     * String → StudentNumberVo 변환 (검색용)
     * Query에서 학번 검색 조건을 VO로 변환
     */
    public StudentNumberVo toSearchStudentNumberVo(String studentNumber) {
        // StudentNumberVo 생성자에서 검증 수행 (null 체크, 형식 검증 등)
        return studentNumber != null ? new StudentNumberVo(studentNumber) : null;
    }

    /**
     * String → NameVo 변환 (검색용)
     * Query에서 이름 검색 조건을 VO로 변환
     */
    public NameVo toSearchNameVo(String name) {
        // NameVo.of() 내부에서 검증 수행 (null 체크, 길이 검증, 형식 검증 등)
        return name != null ? NameVo.of(name) : null;
    }

    /**
     * java.util.List<String> → SkillsVo 변환 (필터링용)
     * Query에서 기술 스택 필터 조건을 VO로 변환
     */
    public SkillsVo toSearchSkillsVo(java.util.List<String> skills) {
        // SkillsVo 생성자에서 검증 수행 (null 체크, 리스트 크기 검증, 중복 제거 등)
        return skills != null && !skills.isEmpty() ? new SkillsVo(skills) : null;
    }
}