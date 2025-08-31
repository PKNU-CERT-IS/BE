package org.certis.studyplatform.member.domain.vo;

/**
 * 프로필 수정 Value Object
 *
 * 프로필 정보 수정 시 사용되는 정보를 담는 불변 객체
 * null 값은 해당 필드를 수정하지 않음을 의미
 */
public record ProfileUpdateVo(
        NameVo name,                      // 이름 (선택적)
        String description,               // 설명 (선택적)
        ProfileImageVo profileImage       // 프로필 이미지 (선택적)
) {

    /**
     * 팩토리 메서드 (모든 필드 포함)
     */
    public static ProfileUpdateVo of(NameVo name, String description, ProfileImageVo profileImage) {
        return new ProfileUpdateVo(name, description, profileImage);
    }

    /**
     * 팩토리 메서드 (이름만)
     */
    public static ProfileUpdateVo nameOnly(NameVo name) {
        return new ProfileUpdateVo(name, null, null);
    }

    /**
     * 팩토리 메서드 (설명만)
     */
    public static ProfileUpdateVo descriptionOnly(String description) {
        return new ProfileUpdateVo(null, description, null);
    }

    /**
     * 팩토리 메서드 (프로필 이미지만)
     */
    public static ProfileUpdateVo profileImageOnly(ProfileImageVo profileImage) {
        return new ProfileUpdateVo(null, null, profileImage);
    }

    /**
     * Builder 패턴
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private NameVo name;
        private String description;
        private ProfileImageVo profileImage;

        public Builder name(NameVo name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder profileImage(ProfileImageVo profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public ProfileUpdateVo build() {
            return new ProfileUpdateVo(name, description, profileImage);
        }
    }

    // =================================================================
    // 업데이트 여부 확인 메서드들
    // =================================================================

    /**
     * 이름 업데이트가 있는지 확인
     */
    public boolean hasNameUpdate() {
        return name != null;
    }

    /**
     * 설명 업데이트가 있는지 확인
     */
    public boolean hasDescriptionUpdate() {
        return description != null;
    }

    /**
     * 프로필 이미지 업데이트가 있는지 확인
     */
    public boolean hasProfileImageUpdate() {
        return profileImage != null;
    }

    /**
     * 어떤 업데이트라도 있는지 확인
     */
    public boolean hasAnyUpdate() {
        return hasNameUpdate() || hasDescriptionUpdate() || hasProfileImageUpdate();
    }

    // =================================================================
    // Getter 메서드들 (Infrastructure Layer에서 사용)
    // =================================================================

    /**
     * 이름 문자열 반환 (nullable)
     */
    public String getNameValue() {
        return name != null ? name.value() : null;
    }

    /**
     * 프로필 이미지 URL 반환 (nullable)
     */
    public String getProfileImageValue() {
        return profileImage != null ? profileImage.value() : null;
    }
}