package org.certis.studyplatform.member.domain.vo;

import java.util.Arrays;
import java.util.List;

/**
 * Gender Value Object
 * 
 * ✅ 성별 정보를 담는 Value Object
 * ✅ MALE, FEMALE, OTHER 값만 허용
 * ✅ 유효성 검사 포함
 */
public record GenderVo(String value) {

    private static final List<String> VALID_GENDERS = Arrays.asList("MALE", "FEMALE", "OTHER");

    public GenderVo {
        // record의 정규화 생성자에서 유효성 검사는 불필요
        // 정적 팩토리 메서드에서 검증됨
    }

    /**
     * 문자열로부터 GenderVo 생성
     */
    public static GenderVo of(String gender) {
        validateGender(gender);
        return new GenderVo(gender.toUpperCase());
    }

    /**
     * 성별 유효성 검사
     */
    private static void validateGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("성별은 null이거나 빈 문자열일 수 없습니다.");
        }

        if (!VALID_GENDERS.contains(gender.toUpperCase())) {
            throw new IllegalArgumentException("유효하지 않은 성별입니다. 허용된 값: " + VALID_GENDERS);
        }
    }

    /**
     * 성별 문자열 반환
     */
    public String getGender() {
        return value;
    }

    /**
     * 남성인지 확인
     */
    public boolean isMale() {
        return "MALE".equals(value);
    }

    /**
     * 여성인지 확인
     */
    public boolean isFemale() {
        return "FEMALE".equals(value);
    }

    /**
     * 기타인지 확인
     */
    public boolean isOther() {
        return "OTHER".equals(value);
    }
} 