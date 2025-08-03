package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.List;

/**
 * 기술 스택 Value Object
 * 
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 기술 스택 관리 규칙을 도메인에서 처리
 */
@Embeddable
public record SkillsVo(List<String> values) {

    public SkillsVo {
        validateSkills(values);
    }

    public String[] toArray() {
        return values.toArray(new String[0]);
    }

    public static SkillsVo of(String... skills) {
        return new SkillsVo(List.of(skills));
    }

    public static SkillsVo of(List<String> skills) {
        return new SkillsVo(List.copyOf(skills));
    }

    public static SkillsVo empty() {
        return new SkillsVo(List.of());
    }
    
    /**
     * 기술 스택 비즈니스 규칙 검증
     */
    private static void validateSkills(List<String> skills) {
        if (skills == null) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                    "기술 스택은 필수입니다");
        }
        
        if (skills.isEmpty()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                    "최소 1개 이상의 기술 스택이 필요합니다");
        }
        
        if (skills.size() > 20) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                    "기술 스택은 최대 20개까지 가능합니다");
        }
        
        // 각 기술 스택 항목 검증
        for (String skill : skills) {
            if (skill == null || skill.trim().isEmpty()) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                        "기술 스택 항목은 빈 값일 수 없습니다");
            }
            
            String trimmedSkill = skill.trim();
            
            if (trimmedSkill.length() > 50) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                        "기술 스택 항목은 50자 이하여야 합니다: " + trimmedSkill);
            }
            
            if (!trimmedSkill.matches("^[가-힣a-zA-Z0-9\\s\\-_.#+]+$")) {
                throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                        "기술 스택 항목은 한글, 영문, 숫자, 공백, 하이픈, 밑줄, 점, 샵, 플러스만 포함할 수 있습니다: " + trimmedSkill);
            }
        }
        
        // 중복 검증
        long distinctCount = skills.stream().map(String::trim).distinct().count();
        if (distinctCount != skills.size()) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_INVALID_SKILLS, 
                    "중복된 기술 스택이 있습니다");
        }
    }
} 