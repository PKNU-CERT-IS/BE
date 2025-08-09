package org.certis.studyplatform.auth.application.object.command;

public record LogoutCommand(Long memberId) {
    public static LogoutCommand of(Long memberId) {
        return new LogoutCommand(memberId);
    }
}