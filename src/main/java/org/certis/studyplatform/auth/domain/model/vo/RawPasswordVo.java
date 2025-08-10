package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record RawPasswordVo(String value) {

    public RawPasswordVo {
        validatePasswordPolicy(value);
    }

    public static RawPasswordVo of(String value) {
        return new RawPasswordVo(value);
    }

    /**
     * 비밀번호 정책 검증
     */
    private static void validatePasswordPolicy(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "비밀번호는 필수입니다");
        }

        // 1. 길이 검증 (기본 정책)
        if (password.length() < 8 || password.length() > 20) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "비밀번호는 8자 이상 20자 이하여야 합니다");
        }

        // 2. 복잡성 검증 (영문자, 숫자, 특수문자 각각 포함)
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$")) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "비밀번호는 영문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다");
        }

        // 3. 일반적인 취약 패스워드 체크
        if (password.toLowerCase().contains("password") ||
                password.contains("123456") || password.contains("qwerty") ||
                password.contains("admin") || password.contains("user")) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "너무 간단한 비밀번호입니다");
        }


        // 4. 반복 문자 체크 (같은 문자 3개 이상)
        if (hasRepeatingCharacters(password, 3)) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "같은 문자가 3개 이상 연속될 수 없습니다");
        }

        // 5. 키보드 패턴 체크
        if (hasKeyboardPattern(password)) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD,
                    "키보드 패턴을 사용할 수 없습니다");
        }
    }


    /**
     * 반복 문자 체크 (aaa, 111 등)
     */
    private static boolean hasRepeatingCharacters(String password, int length) {
        for (int i = 0; i <= password.length() - length; i++) {
            char firstChar = password.charAt(i);
            boolean isRepeating = true;
            for (int j = 1; j < length; j++) {
                if (password.charAt(i + j) != firstChar) {
                    isRepeating = false;
                    break;
                }
            }
            if (isRepeating) return true;
        }
        return false;
    }

    /**
     * 키보드 패턴 체크 (qwerty, asdf 등) 나중에 추가나 따로 패턴 라이브러리 사용하는게 좋을
     */
    private static boolean hasKeyboardPattern(String password) {
        String[] patterns = {
                "qwerty", "qwertyuiop", "asdf", "asdfghjkl", "zxcv", "zxcvbnm",
                "1234", "12345", "123456", "1234567890",
        };

        String lowerPassword = password.toLowerCase();
        for (String pattern : patterns) {
            if (lowerPassword.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}