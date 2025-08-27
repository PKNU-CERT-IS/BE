package org.certis.studyplatform.member.domain.vo;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;

/**
 * 생년월일 Value Object
 *
 * 회원의 생년월일 정보를 담는 불변 객체
 * 나이 계산 등의 비즈니스 로직 포함
 */
public record BirthdayVo(OffsetDateTime value) {

    public BirthdayVo {
        if (value == null) {
            throw new IllegalArgumentException("생년월일은 필수입니다");
        }

        // 미래 날짜 검증
        if (value.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("생년월일은 미래일 수 없습니다");
        }

        // 너무 과거 날짜 검증 (100년 전까지만 허용)
        OffsetDateTime hundredYearsAgo = OffsetDateTime.now().minusYears(100);
        if (value.isBefore(hundredYearsAgo)) {
            throw new IllegalArgumentException("생년월일이 너무 과거입니다");
        }
    }

    /**
     * 팩토리 메서드
     */
    public static BirthdayVo of(OffsetDateTime birthday) {
        return new BirthdayVo(birthday);
    }

    /**
     * 현재 나이 계산
     */
    public int getAge() {
        return Period.between(value.toLocalDate(), LocalDateTime.now().toLocalDate()).getYears();
    }

    /**
     * 성인 여부 확인
     */
    public boolean isAdult() {
        return getAge() >= 18;
    }

    /**
     * 생년월일 포맷 문자열 반환
     */
    public String getFormattedBirthday() {
        return value.toLocalDate().toString();
    }
}