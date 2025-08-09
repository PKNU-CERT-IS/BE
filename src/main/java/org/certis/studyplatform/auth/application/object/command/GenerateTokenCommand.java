package org.certis.studyplatform.auth.application.object.command;

import org.certis.studyplatform.member.domain.MemberRole;

public record GenerateTokenCommand(Long memberId, MemberRole role) {
    public static GenerateTokenCommand of(Long memberId, MemberRole role) {
        return new GenerateTokenCommand(memberId, role);
    }
}
