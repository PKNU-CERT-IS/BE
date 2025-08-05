package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.List;

/**
 * 기술 스택 Value Object.
 * 생성 시점에 비즈니스 규칙을 검증하여 항상 유효한 상태를 보장합니다.
 */
public record SkillsVo(List<String> values) {

    /**
     * 정식 생성자(Canonical Constructor)에서 유효성 검사를 호출합니다.
     */
    public SkillsVo {
        validateSkills(values);
    }

    /**
     * List<String>을 받아 SkillsVo 객체를 생성하는 정적 팩토리 메서드입니다.
     * @param skills 기술 스택 목록
     * @return 새로운 SkillsVo 인스턴스
     */
    public static SkillsVo of(List<String> skills) {
        // 불변 리스트로 방어적 복사를 수행하여 안정성을 높입니다.
        return new SkillsVo(List.copyOf(skills));
    }

    /**
     * 기술 스택의 비즈니스 규칙을 검증합니다.
     * @param skills 검증할 기술 스택 목록
     */
    private static void validateSkills(List<String> skills) {
        if (skills == null) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                    "기술 스택 목록은 null일 수 없습니다.");
        }

        // [핵심 규칙] 비어있는 리스트를 허용하지 않습니다.
        if (skills.isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                    "최소 1개 이상의 기술 스택이 필요합니다.");
        }

        if (skills.size() > 20) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                    "기술 스택은 최대 20개까지 등록할 수 있습니다.");
        }

        // 각 기술 스택 항목 검증
        for (String skill : skills) {
            if (skill == null || skill.trim().isEmpty()) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                        "기술 스택 항목은 빈 값일 수 없습니다.");
            }

            String trimmedSkill = skill.trim();

            if (trimmedSkill.length() > 50) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                        "기술 스택 항목은 50자 이하여야 합니다: " + trimmedSkill);
            }

            if (!trimmedSkill.matches("^[가-힣a-zA-Z0-9\\s\\-_.#+]+$")) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                        "기술 스택 항목에 허용되지 않는 문자가 포함되어 있습니다: " + trimmedSkill);
            }
        }

        // 중복 검증
        long distinctCount = skills.stream().map(String::trim).distinct().count();
        if (distinctCount != skills.size()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS,
                    "중복된 기술 스택 항목이 존재합니다.");
        }
    }
}
