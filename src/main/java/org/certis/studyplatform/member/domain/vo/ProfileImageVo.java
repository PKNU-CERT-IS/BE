package org.certis.studyplatform.member.domain.vo;

/**
 * 프로필 이미지 Value Object (Record)
 *
 * Record를 사용하여 불변성과 간결성을 보장
 * 프로필 이미지 URL을 도메인 객체로 래핑
 */
public record ProfileImageVo(String value) {

    /**
     * 정규화 생성자 - 유효성 검증
     */
    public ProfileImageVo {
        // null이나 빈 문자열 처리
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
    }

    /**
     * 정적 팩토리 메서드
     */
    public static ProfileImageVo of(String url) {
        return new ProfileImageVo(url);
    }

    /**
     * 빈 프로필 이미지 생성
     */
    public static ProfileImageVo empty() {
        return new ProfileImageVo(null);
    }

    /**
     * 프로필 이미지가 있는지 확인
     */
    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }

    /**
     * 프로필 이미지가 비어있는지 확인
     */
    public boolean isEmpty() {
        return !hasValue();
    }

    /**
     * URL이 유효한 형식인지 간단 검증
     */
    public boolean isValidUrl() {
        if (!hasValue()) return false;
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /**
     * 이미지 파일 확장자인지 확인
     */
    public boolean isImageFile() {
        if (!hasValue()) return false;
        String lowercaseUrl = value.toLowerCase();
        return lowercaseUrl.endsWith(".jpg") ||
                lowercaseUrl.endsWith(".jpeg") ||
                lowercaseUrl.endsWith(".png") ||
                lowercaseUrl.endsWith(".gif") ||
                lowercaseUrl.endsWith(".webp");
    }

    /**
     * 안전한 값 반환 (null 대신 빈 문자열)
     */
    public String valueOrEmpty() {
        return value != null ? value : "";
    }

    /**
     * 기본 이미지 URL과 함께 값 반환
     */
    public String valueOrDefault(String defaultUrl) {
        return hasValue() ? value : defaultUrl;
    }
}