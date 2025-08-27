package org.certis.studyplatform.member.domain;

import lombok.Getter;

@Getter
public enum MemberGrade {
    FRESHMAN("1학년"),
    SOPHOMORE("2학년"),
    JUNIOR("3학년"),
    SENIOR("4학년"),
    GRADUATED("졸업생"),
    NONE("미지정");

    private final String description;

    MemberGrade(String description) {
        this.description = description;
    }

    /**
     * 설명으로부터 MemberGrade 찾기
     */
    public static MemberGrade fromDescription(String description) {
        if (description == null) {
            return NONE;
        }

        for (MemberGrade grade : values()) {
            if (grade.description.equals(description)) {
                return grade;
            }
        }
        return NONE;
    }

    /**
     * 학년 문자열로부터 MemberGrade 찾기 (데이터베이스 호환성 포함)
     */
    public static MemberGrade fromGradeString(String gradeString) {
        if (gradeString == null || gradeString.trim().isEmpty()) {
            return NONE;
        }

        String trimmed = gradeString.trim().toUpperCase();

        // 먼저 enum 값으로 직접 매칭 시도
        try {
            return MemberGrade.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // 데이터베이스 값과 enum 값 매핑
            return switch (trimmed) {
                case "1학년", "1" -> FRESHMAN;
                case "2학년", "2" -> SOPHOMORE;
                case "3학년", "3" -> JUNIOR;
                case "4학년", "4" -> SENIOR;
                case "졸업생", "졸업", "수료생" -> GRADUATED;
                // 데이터베이스에서 직접 오는 enum 문자열 값들 처리
                case "FRESHMAN" -> FRESHMAN;
                case "SOPHOMORE" -> SOPHOMORE;
                case "JUNIOR" -> JUNIOR;
                case "SENIOR" -> SENIOR;
                case "GRADUATED" -> GRADUATED;
                case "NONE" -> NONE;
                default -> NONE;
            };
        }
    }

    public boolean isFreshman() {
        return this == FRESHMAN;
    }

    public boolean isSophomore() {
        return this == SOPHOMORE;
    }

    public boolean isJunior() {
        return this == JUNIOR;
    }

    public boolean isSenior() {
        return this == SENIOR;
    }

    public boolean isGraduated() {
        return this == GRADUATED;
    }

    public boolean isUndergraduate() {
        return this == FRESHMAN || this == SOPHOMORE || this == JUNIOR || this == SENIOR;
    }
}
