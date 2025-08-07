package org.certis.studyplatform.member.domain.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Birthday Value Object
 * 
 * ✅ 생년월일 정보를 담는 Value Object
 * ✅ YYYY-MM-DD 형식의 문자열을 받아 LocalDateTime로 변환
 * ✅ 유효성 검사 포함
 */
public record BirthdayVo(LocalDateTime value) {

    public BirthdayVo {
        // record의 정규화 생성자에서 유효성 검사는 불필요
        // LocalDateTime는 이미 유효한 날짜만 생성됨
    }

    /**
     * 문자열로부터 BirthdayVo 생성
     */
    public static BirthdayVo of(String birthday) {
        validateBirthday(birthday);
        LocalDateTime localDate = parseBirthday(birthday);
        return new BirthdayVo(localDate);
    }

    /**
     * LocalDateTime로부터 BirthdayVo 생성
     */
    public static BirthdayVo of(LocalDateTime birthday) {
        return new BirthdayVo(birthday);
    }

    /**
     * 생년월일 유효성 검사
     */
    private static void validateBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            throw new IllegalArgumentException("생년월일은 null이거나 빈 문자열일 수 없습니다.");
        }

        if (!birthday.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("생년월일은 YYYY-MM-DD 형식이어야 합니다.");
        }
    }

    /**
     * 문자열을 LocalDateTime로 파싱
     */
    private static LocalDateTime parseBirthday(String birthday) {
        try {
            return LocalDateTime.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 생년월일 형식입니다: " + birthday);
        }
    }

    /**
     * LocalDateTime 반환
     */
    public LocalDateTime getBirthday() {
        return value;
    }

    /**
     * 문자열 형태로 반환 (YYYY-MM-DD)
     */
    public String getBirthdayAsString() {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
} 