package org.certis.studyplatform.member.domain.vo;

import java.util.Set;

/**
 * 성별 Value Object
 * 
 * 회원의 성별 정보를 담는 불변 객체
 * 허용된 성별 값들에 대한 검증 포함
 */
public record GenderVo(String value) {
    
    // 허용된 성별 값들
    private static final Set<String> VALID_GENDERS = Set.of(
        "MALE",     // 남성
        "FEMALE",   // 여성
        "OTHER",    // 기타
        "UNKNOWN"   // 미지정
    );
    
    public GenderVo {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("성별은 필수입니다");
        }
        
        String upperValue = value.trim().toUpperCase();
        if (!VALID_GENDERS.contains(upperValue)) {
            throw new IllegalArgumentException("유효하지 않은 성별입니다. 허용값: " + VALID_GENDERS);
        }
        
        // 정규화된 값으로 저장
        value = upperValue;
    }
    
    /**
     * 팩토리 메서드
     */
    public static GenderVo of(String gender) {
        return new GenderVo(gender);
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
     * 성별이 지정되었는지 확인
     */
    public boolean isSpecified() {
        return !"UNKNOWN".equals(value);
    }
    
    /**
     * 표시용 문자열 반환
     */
    public String getDisplayName() {
        return switch (value) {
            case "MALE" -> "남성";
            case "FEMALE" -> "여성";
            case "OTHER" -> "기타";
            case "UNKNOWN" -> "미지정";
            default -> value;
        };
    }
} 