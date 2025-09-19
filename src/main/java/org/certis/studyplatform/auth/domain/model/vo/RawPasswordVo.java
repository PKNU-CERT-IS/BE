package org.certis.studyplatform.auth.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record RawPasswordVo(String value) {

    public RawPasswordVo {
        validatePasswordLength(value);
    }

    public static RawPasswordVo of(String value) {
        return new RawPasswordVo(value);
    }

    /**
     * 비밀번호 정책 검증
     */
    private static void validatePasswordLength(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD);
        }
        if (password.length() > 255) {
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_WEAK_PASSWORD);
        }
    }
//
//
//    /**
//     * 반복 문자 체크 (aaa, 111 등)
//     */
//    private static boolean hasRepeatingCharacters(String password, int length) {
//        for (int i = 0; i <= password.length() - length; i++) {
//            char firstChar = password.charAt(i);
//            boolean isRepeating = true;
//            for (int j = 1; j < length; j++) {
//                if (password.charAt(i + j) != firstChar) {
//                    isRepeating = false;
//                    break;
//                }
//            }
//            if (isRepeating) return true;
//        }
//        return false;
//    }
//
//    /**
//     * 키보드 패턴 체크 (qwerty, asdf 등) 나중에 추가나 따로 패턴 라이브러리 사용하는게 좋을
//     */
//    private static boolean hasKeyboardPattern(String password) {
//        String[] patterns = {
//                "qwerty", "qwertyuiop", "asdf", "asdfghjkl", "zxcv", "zxcvbnm",
//                "1234", "12345", "123456", "1234567890",
//        };
//
//        String lowerPassword = password.toLowerCase();
//        for (String pattern : patterns) {
//            if (lowerPassword.contains(pattern)) {
//                return true;
//            }
//        }
//        return false;
//    }
}