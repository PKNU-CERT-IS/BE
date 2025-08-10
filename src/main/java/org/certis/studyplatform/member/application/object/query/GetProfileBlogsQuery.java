package org.certis.studyplatform.member.application.object.query;

import org.springframework.data.domain.Pageable;

public record GetProfileBlogsQuery(
        Long memberId,
        boolean publishedOnly,
        Pageable pageable
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long memberId;
        private boolean publishedOnly = true;
        private Pageable pageable;

        public Builder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder publishedOnly(boolean publishedOnly) {
            this.publishedOnly = publishedOnly;
            return this;
        }

        public Builder pageable(Pageable pageable) {
            this.pageable = pageable;
            return this;
        }

        public GetProfileBlogsQuery build() {
            return new GetProfileBlogsQuery(memberId, publishedOnly, pageable);
        }
    }
}