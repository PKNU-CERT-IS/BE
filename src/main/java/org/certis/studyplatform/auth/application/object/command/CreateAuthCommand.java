package org.certis.studyplatform.auth.application.object.command;

public record CreateAuthCommand(
        Long memberId,           // Member 생성 후 받은 ID
        String accountNumber,    // 로그인 ID
        String password          // 원시 비밀번호
) {}