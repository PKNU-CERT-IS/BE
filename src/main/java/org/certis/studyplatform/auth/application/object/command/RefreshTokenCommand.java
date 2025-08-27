package org.certis.studyplatform.auth.application.object.command;

import org.certis.studyplatform.member.domain.MemberRole;

public record RefreshTokenCommand(
        Long memberId,
        String username,    // 학번 (student_number)
        String email,       // 이메일
        String name,        // 이름
        MemberRole role
) {
    public static RefreshTokenCommand of(Long memberId, String username, String email, String name, MemberRole role) {
        return new RefreshTokenCommand(memberId, username, email, name, role);
    }
}