package org.certis.studyplatform.member.application.object.command;

public record UpdateProfileCommand(
        Long memberId,
        String name,
        String description,
        String profileImage
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long memberId;
        private String name;
        private String description;
        private String profileImage;

        public Builder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder profileImage(String profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public UpdateProfileCommand build() {
            return new UpdateProfileCommand(memberId, name, description, profileImage);
        }
    }
}