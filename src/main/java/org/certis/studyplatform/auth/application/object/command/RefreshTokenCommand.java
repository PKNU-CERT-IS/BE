package org.certis.studyplatform.auth.application.object.command;

import org.certis.studyplatform.member.domain.MemberRole;

public record RefreshTokenCommand(Long memberId, MemberRole role) {
    public static RefreshTokenCommand of(Long memberId, MemberRole role) {
        return new RefreshTokenCommand(memberId, role);
    }
}