package org.certis.studyplatform.auth.application.object.query;

public record ValidateRefreshTokenQuery(Long memberId) {
    public static ValidateRefreshTokenQuery of(Long memberId) {
        return new ValidateRefreshTokenQuery(memberId);
    }
}